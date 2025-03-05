package com.example.swipebyte.data.repository

import android.util.Log
import com.example.swipebyte.ui.data.models.YelpResponse
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.YelpBusiness
import com.example.swipebyte.ui.data.models.YelpBusinessDetailsResponse
import com.example.swipebyte.ui.data.models.YelpCategory
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
        firestoreSettings = firestoreSettings.toBuilder().setPersistenceEnabled(true).build()
    }
    private val restaurantCollection = db.collection("restaurants")
    private val yelpApiKey = "pmie5_FVr0xgJsJyZWnmVRKF2WoTPQFH7iOaO7CUTMoQeqDlX54gvf0ql4ZbS89usMdSrExV9nbsmIXiYN7_h-RNWguknSTJ_KlwGsfaDEwnpOrssaBEwXqs_-XFZ3Yx"

    // Modified getRestaurants: first try loading cached data from Firestore using Source.CACHE.
    suspend fun getRestaurants(forceRefresh: Boolean = false): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                try {
                    val cacheSnapshot = restaurantCollection.get(Source.CACHE).await()
                    if (!cacheSnapshot.isEmpty) {
                        Log.d("RestaurantRepo", "Loaded ${cacheSnapshot.size()} restaurants from cache")
                        return@withContext cacheSnapshot.documents.mapNotNull { doc ->
                            mapDocumentToRestaurant(doc)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RestaurantRepo", "Error fetching from cache: ${e.message}")
                }
            }
            // Either forceRefresh is true or cache is empty – fetch from Yelp.
            try {
                val yelpRestaurants = fetchFromYelp()
                val detailRestaurants = yelpRestaurants.map { restaurant ->
                    async {
                        fetchBusinessDetailsOptimized(restaurant)
                    }
                }.awaitAll()
                // Update Firebase asynchronously
                storeInFirebase(detailRestaurants)
                detailRestaurants
            } catch (e: Exception) {
                Log.e("RestaurantRepo", "Error fetching from Yelp: ${e.message}")
                // Fallback to Firestore SERVER if Yelp fails
                try {
                    val snapshot = restaurantCollection.get(Source.SERVER).await()
                    snapshot.documents.mapNotNull { doc ->
                        mapDocumentToRestaurant(doc)
                    }
                } catch (fallbackE: Exception) {
                    Log.e("RestaurantRepo", "Error fetching from Firestore fallback: ${fallbackE.message}")
                    emptyList()
                }
            }
        }
    }

    private suspend fun fetchFromYelp(): List<Restaurant> {
        val response = yelpAPI.getRestaurants("Toronto", authHeader = "Bearer $yelpApiKey")
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

    // New: Realtime updates using snapshot listener via Kotlin Flow.
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

data class YelpResponse(
    val businesses: List<YelpBusiness>
)

data class YelpBusiness(
    val id: String,
    val name: String,
    val rating: Double,
    val price: String?,
    val image_url: String?,
    val categories: List<YelpCategory>,
    val coordinates: YelpCoordinates,
    val location: YelpLocation?,
    val url: String?,
    val phone: String?,
    val distance: Double = 0.0
)

data class YelpCategory(
    val title: String
)

data class YelpCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class YelpLocation(
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val state: String?,
    val zip_code: String?
)
