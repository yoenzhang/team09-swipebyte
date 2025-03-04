package com.example.swipebyte.data.repository

import android.util.Log
import com.example.swipebyte.ui.data.models.YelpResponse
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.YelpBusiness
import com.example.swipebyte.ui.data.models.YelpCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// --- Yelp API Service ---
interface YelpAPI {
    @GET("businesses/search")
    suspend fun getRestaurants(
        @Query("location") location: String,
        @Query("limit") limit: Int = 50,
        @Header("Authorization") authHeader: String
    ): YelpResponse
}

// --- Retrofit Instance ---
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.yelp.com/v3/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val yelpAPI = retrofit.create(YelpAPI::class.java)

class RestaurantRepository {
    private val db = FirebaseFirestore.getInstance()
    private val restaurantCollection = db.collection("restaurants")
    private val yelpApiKey = "pmie5_FVr0xgJsJyZWnmVRKF2WoTPQFH7iOaO7CUTMoQeqDlX54gvf0ql4ZbS89usMdSrExV9nbsmIXiYN7_h-RNWguknSTJ_KlwGsfaDEwnpOrssaBEwXqs_-XFZ3Yx"

    suspend fun getRestaurants(): List<Restaurant> {
        try {
            // Always fetch from Yelp to update the database
            val yelpRestaurants = fetchFromYelp()

            // Store in Firebase (this happens in the background)
            storeInFirebase(yelpRestaurants)

            // Return the Yelp data directly while Firebase updates in the background
            return yelpRestaurants
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error fetching from Yelp: ${e.message}")

            // Fallback to Firebase if Yelp fails
            try {
                val snapshot = restaurantCollection.limit(50).get().await()
                return snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Restaurant::class.java)
                }
            } catch (e: Exception) {
                Log.e("RestaurantRepo", "Error fetching from Firebase: ${e.message}")
                return emptyList()
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
                address = formatAddress(business),
                //location2 = business.location.map {it.address1}
                //address = formatAddress(business),
                //distance = business.distance / 1000 // Convert from meters to kilometers
            )
        }
    }

    private fun formatAddress(business: YelpBusiness): String {
        val location = business.location
        val addressParts = mutableListOf<String>()

        // Add address1 if available
        location.address1?.let { if (it.isNotEmpty()) addressParts.add(it) }

        // Add city, state, zip if available
        if (location.city != null && location.state != null && location.zip_code != null) {
            addressParts.add("${location.city}, ${location.state} ${location.zip_code}")
        }

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            "Address not available"
        }
    }

    private suspend fun storeInFirebase(restaurants: List<Restaurant>) {
        try {
            // Use a batch to update multiple documents efficiently
            val batch = db.batch()

            restaurants.forEach { restaurant ->
                val docRef = restaurantCollection.document(restaurant.id)
                batch.set(docRef, restaurant)
            }

            batch.commit().await()
            Log.d("RestaurantRepo", "Successfully updated ${restaurants.size} restaurants in Firebase")
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error storing in Firebase: ${e.message}")
        }
    }

//    private fun formatAddress(business: YelpBusiness): String {
//        val location = business.location
//        return if (location != null) {
//            val addressParts = mutableListOf<String>()
//            location.address1?.let { if (it.isNotEmpty()) addressParts.add(it) }
//
//            if (location.city != null && location.state != null && location.zip_code != null) {
//                addressParts.add("${location.city}, ${location.state} ${location.zip_code}")
//            }
//
//            addressParts.joinToString(", ")
//        } else {
//            "Address not available"
//        }
//    }
}

// Updated model classes to match Yelp API response format
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