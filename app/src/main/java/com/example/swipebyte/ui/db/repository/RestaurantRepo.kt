package com.example.swipebyte.data.repository

import android.content.Context
import android.util.Log
import com.example.swipebyte.ui.data.models.YelpResponse
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.YelpBusiness
import com.example.swipebyte.ui.data.models.YelpBusinessDetailsResponse
import com.example.swipebyte.ui.data.models.YelpCategory
import com.example.swipebyte.ui.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// --- Yelp API Service ---
interface YelpAPI {
    @GET("businesses/search")
    suspend fun getRestaurants(
        @Query("location") location: String,
        @Query("limit") limit: Int = 50,
        @Query("price") price: String? = null, // Optional price filter
        @Query("categories") categories: String? = null, // Optional categories filter
        @Header("Authorization") authHeader: String
    ): YelpResponse

    @GET("businesses/{id}")
    suspend fun getBusinessDetails(
        @Path("id") id: String,
        @Header("Authorization") authHeader: String
    ): YelpBusinessDetailsResponse
}

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.yelp.com/v3/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val yelpAPI = retrofit.create(YelpAPI::class.java)

class RestaurantRepository {
    private val db = FirebaseFirestore.getInstance().apply {
        // Enable offline persistence for faster local access
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestoreSettings = settings
    }
    private val restaurantCollection = db.collection("restaurants")
    private val usersCollection = db.collection("users")
    private val yelpApiKey = "pmie5_FVr0xgJsJyZWnmVRKF2WoTPQFH7iOaO7CUTMoQeqDlX54gvf0ql4ZbS89usMdSrExV9nbsmIXiYN7_h-RNWguknSTJ_KlwGsfaDEwnpOrssaBEwXqs_-XFZ3Yx"

    // Get current user preferences
    private suspend fun getUserPreferences(): User? {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return null

        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error fetching user preferences: ${e.message}")
            null
        }
    }

    // Convert price preferences from strings (e.g., "$", "$$") to integers for Yelp API (1, 2)
    private fun convertPricePreferencesToNumbers(pricePreferences: List<String>): List<Int> {
        return pricePreferences.mapNotNull { price ->
            when (price) {
                "$" -> 1
                "$$" -> 2
                "$$$" -> 3
                "$$$$" -> 4
                else -> null
            }
        }
    }

    // Calculate distance between two points using Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    // Main function to get restaurants with user preferences and distance filter
    suspend fun getRestaurants(
        forceRefresh: Boolean = false,
        overridePrice: List<Int> = emptyList(),
        overrideCategories: List<String> = emptyList(),
        useUserPreferences: Boolean = true,
        maxDistance: Double? = null, // New parameter for distance filtering
        context: Context? = null
    ): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            // Fetch user preferences if needed
            val userPrefs = if (useUserPreferences) getUserPreferences() else null

            // Get user location
            val userLocation = userPrefs?.location
            val userLatitude = userLocation?.latitude ?: 0.0
            val userLongitude = userLocation?.longitude ?: 0.0

            // Get max distance from parameter or preferences
            val effectiveMaxDistance = maxDistance ?: run {
                if (context != null) {
                    val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.getFloat("location_radius", 5.0f).toDouble()
                } else {
                    5.0 // Default radius if context not available
                }
            }

            Log.d("RestaurantRepo", "Getting restaurants with distance filter: $effectiveMaxDistance km")

            // Use provided filters or fall back to user preferences
            val priceFilters = if (overridePrice.isNotEmpty()) {
                overridePrice
            } else if (useUserPreferences && userPrefs?.pricePreferences?.isNotEmpty() == true) {
                convertPricePreferencesToNumbers(userPrefs.pricePreferences)
            } else {
                emptyList()
            }

            val categoryFilters = if (overrideCategories.isNotEmpty()) {
                overrideCategories
            } else if (useUserPreferences && userPrefs?.cuisinePreferences?.isNotEmpty() == true) {
                userPrefs.cuisinePreferences
            } else {
                emptyList()
            }

            // Get user location or default to Toronto
            val location = if (useUserPreferences &&
                userPrefs?.location != null &&
                userPrefs.location.latitude != 0.0 &&
                userPrefs.location.longitude != 0.0) {
                "${userPrefs.location.latitude},${userPrefs.location.longitude}"
            } else {
                "Toronto" // Default location
            }

            if (!forceRefresh) {
                try {
                    val cacheSnapshot = restaurantCollection.get(Source.CACHE).await()
                    if (!cacheSnapshot.isEmpty) {
                        Log.d("RestaurantRepo", "Loaded ${cacheSnapshot.size()} restaurants from cache")
                        val restaurants = cacheSnapshot.documents.mapNotNull { doc ->
                            mapDocumentToRestaurant(doc)
                        }

                        // Apply filters including distance filter
                        val filteredRestaurants = filterRestaurants(
                            restaurants,
                            priceFilters,
                            categoryFilters,
                            userLatitude,
                            userLongitude,
                            effectiveMaxDistance
                        )

                        Log.d("RestaurantRepo", "Filtered to ${filteredRestaurants.size} restaurants within ${effectiveMaxDistance}km")
                        return@withContext filteredRestaurants
                    }
                } catch (e: Exception) {
                    Log.e("RestaurantRepo", "Error fetching from cache: ${e.message}")
                }
            }

            // Either forceRefresh is true or cache is empty – fetch from Yelp.
            try {
                val yelpRestaurants = fetchFromYelp(priceFilters, categoryFilters, location)

                // Calculate and set distance for each restaurant
                yelpRestaurants.forEach { restaurant ->
                    if (userLatitude != 0.0 && userLongitude != 0.0) {
                        val distance = calculateDistance(
                            userLatitude,
                            userLongitude,
                            restaurant.location.latitude,
                            restaurant.location.longitude
                        )
                        restaurant.distance = distance
                    }
                }

                val detailRestaurants = yelpRestaurants.map { restaurant ->
                    async {
                        fetchBusinessDetailsOptimized(restaurant)
                    }
                }.awaitAll()

                // Update Firebase asynchronously
                storeInFirebase(detailRestaurants)

                // Apply distance filter
                val filteredRestaurants = if (effectiveMaxDistance > 0 && userLatitude != 0.0 && userLongitude != 0.0) {
                    detailRestaurants.filter { restaurant ->
                        restaurant.distance <= effectiveMaxDistance
                    }
                } else {
                    detailRestaurants
                }

                Log.d("RestaurantRepo", "Retrieved ${detailRestaurants.size} restaurants, filtered to ${filteredRestaurants.size} within ${effectiveMaxDistance}km")
                filteredRestaurants

            } catch (e: Exception) {
                Log.e("RestaurantRepo", "Error fetching from Yelp: ${e.message}")
                // Fallback to Firestore SERVER if Yelp fails
                try {
                    val snapshot = restaurantCollection.get(Source.SERVER).await()
                    val restaurants = snapshot.documents.mapNotNull { doc ->
                        mapDocumentToRestaurant(doc)
                    }
                    // Apply filters to server results including distance
                    filterRestaurants(
                        restaurants,
                        priceFilters,
                        categoryFilters,
                        userLatitude,
                        userLongitude,
                        effectiveMaxDistance
                    )
                } catch (fallbackE: Exception) {
                    Log.e("RestaurantRepo", "Error fetching from Firestore fallback: ${fallbackE.message}")
                    emptyList()
                }
            }
        }
    }

    // Filter restaurants based on price, categories, and distance
    private fun filterRestaurants(
        restaurants: List<Restaurant>,
        priceFilters: List<Int>,
        categoryFilters: List<String>,
        userLatitude: Double = 0.0,
        userLongitude: Double = 0.0,
        maxDistance: Double = 0.0
    ): List<Restaurant> {
        var filtered = restaurants

        // Apply price filters if any
        if (priceFilters.isNotEmpty()) {
            filtered = filtered.filter { restaurant ->
                val dollarSignCount = restaurant.priceRange?.count { it == '$' } ?: 0
                priceFilters.contains(dollarSignCount)
            }
        }

        // Apply category filters if any
        if (categoryFilters.isNotEmpty()) {
            filtered = filtered.filter { restaurant ->
                restaurant.cuisineType.any { cuisine ->
                    categoryFilters.any { category ->
                        cuisine.contains(category, ignoreCase = true)
                    }
                }
            }
        }

        // Apply distance filter if we have user location and maxDistance
        if (maxDistance > 0 && userLatitude != 0.0 && userLongitude != 0.0) {
            filtered = filtered.filter { restaurant ->
                // Calculate distance if it's not already set
                if (restaurant.distance <= 0.0) {
                    restaurant.distance = calculateDistance(
                        userLatitude,
                        userLongitude,
                        restaurant.location.latitude,
                        restaurant.location.longitude
                    )
                }

                restaurant.distance <= maxDistance
            }
        }

        return filtered
    }

    private suspend fun fetchFromYelp(
        price: List<Int>,
        categories: List<String>,
        location: String
    ): List<Restaurant> {
        // Convert the price list into a comma-separated string if it's not empty.
        val priceFilter = if (price.isNotEmpty()) price.joinToString(",") else null
        // Convert the categories list into a comma-separated string if it's not empty.
        val categoriesFilter = if (categories.isNotEmpty()) categories.joinToString(",") else null

        val response = yelpAPI.getRestaurants(
            location = location,
            authHeader = "Bearer $yelpApiKey",
            price = priceFilter,
            categories = categoriesFilter
        )

        return response.businesses.map { business ->
            Restaurant(
                id = business.id,
                name = business.name,
                yelpRating = business.rating.toFloat(),
                priceRange = business.price ?: "$-$$$$",
                imageUrls = business.image_url?.let { listOf(it) } ?: emptyList(),
                cuisineType = business.categories.map { it.title },
                location = GeoPoint(business.coordinates.latitude, business.coordinates.longitude),
                url = business.url ?: "",
                phone = business.phone ?: "",
                address = formatAddress(business)
            )
        }
    }

    private suspend fun fetchBusinessDetailsOptimized(restaurant: Restaurant): Restaurant {
        return try {
            val response = yelpAPI.getBusinessDetails(restaurant.id, authHeader = "Bearer $yelpApiKey")
            Log.d("RestaurantRepo", "Successfully fetched details for ${restaurant.name}")
            restaurant.copy(
                hours = response.hours ?: restaurant.hours,
                imageUrls = response.photos.ifEmpty { restaurant.imageUrls }
            )
        } catch (e: Exception) {
            Log.w("RestaurantRepo", "Could not fetch additional details for ${restaurant.name}")
            restaurant
        }
    }

    private fun formatAddress(business: YelpBusiness): String {
        val location = business.location
        val addressParts = mutableListOf<String>()
        location?.let {
            it.address1?.takeIf { address -> address.isNotEmpty() }?.let { address -> addressParts.add(address) }
            if (it.city != null && it.state != null && it.zip_code != null) {
                addressParts.add("${it.city}, ${it.state} ${it.zip_code}")
            }
        }
        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            "Address not available"
        }
    }

    private suspend fun storeInFirebase(restaurants: List<Restaurant>) {
        withContext(Dispatchers.IO) {
            try {
                val batch = db.batch()
                restaurants.forEach { restaurant ->
                    val docRef = restaurantCollection.document(restaurant.id)
                    batch.set(docRef, restaurant, SetOptions.merge())
                }
                batch.commit().await()
                Log.d("RestaurantRepo", "Successfully updated ${restaurants.size} restaurants in Firebase")
            } catch (e: Exception) {
                Log.e("RestaurantRepo", "Error storing in Firebase: ${e.message}")
            }
        }
    }

    // Realtime updates using snapshot listener via Kotlin Flow.
    fun getRestaurantsRealtime() = callbackFlow<List<Restaurant>> {
        val registration = restaurantCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            snapshot?.let {
                val restaurants = it.documents.mapNotNull { doc -> mapDocumentToRestaurant(doc) }
                trySend(restaurants).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    // Helper function to safely map a DocumentSnapshot to a Restaurant.
    private fun mapDocumentToRestaurant(doc: DocumentSnapshot): Restaurant? {
        return try {
            val restaurant = doc.toObject(Restaurant::class.java)
            restaurant?.let {
                // Check if the "distance" field is stored as a String and convert if necessary.
                val distanceValue = doc.get("distance")
                if (distanceValue is String) {
                    it.distance = distanceValue.toDoubleOrNull() ?: 0.0
                }
            }
            restaurant
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error deserializing restaurant: ${e.message}")
            null
        }
    }
}