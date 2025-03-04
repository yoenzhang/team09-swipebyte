package com.example.swipebyte.data.repository

import android.util.Log
import com.example.swipebyte.ui.data.models.YelpResponse
import com.example.swipebyte.ui.data.models.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.toObjects
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
        @Query("limit") limit: Int = 10,
        @Header("Authorization") authHeader: String
    ): YelpResponse
}

// --- Retrofit Instance ---
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.yelp.com/v3/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val yelpAPI = retrofit.create(  YelpAPI::class.java)

class RestaurantRepository {
    private val db = FirebaseFirestore.getInstance()
    private val restaurantCollection = db.collection("restaurants")
    private val yelpApiKey = "pmie5_FVr0xgJsJyZWnmVRKF2WoTPQFH7iOaO7CUTMoQeqDlX54gvf0ql4ZbS89usMdSrExV9nbsmIXiYN7_h-RNWguknSTJ_KlwGsfaDEwnpOrssaBEwXqs_-XFZ3Yx"

    suspend fun getRestaurants(): List<Restaurant> {
        try {
            // 1️⃣ Check Firebase for restaurants
            val snapshot = restaurantCollection.limit(10).get().await()
            val restaurants = snapshot.toObjects<Restaurant>()

            return if (restaurants.isNotEmpty()) {
                restaurants // ✅ Return existing restaurants
            } else {
                fetchAndStoreFromYelp() // 🔄 Call Yelp API if Firebase is empty
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error fetching restaurants: ${e.message}")
            return emptyList()
        }
    }

    private suspend fun fetchAndStoreFromYelp(): List<Restaurant> {
        return try {
            // 2️⃣ Call Yelp API
            val response = yelpAPI.getRestaurants("Toronto", authHeader = "Bearer $yelpApiKey")

            // 3️⃣ Store in Firebase
            val restaurantList = response.businesses.map { business ->
                Restaurant(
                    id = business.id,
                    name = business.name,
                    yelpRating = business.rating.toFloat(),
                    priceRange = business.price ?: "$-$$$$",
                    imageUrls = business.image_url?.let { listOf(it) } ?: emptyList(), // Handle multiple images
                    cuisineType = business.categories.map { it.title },
                    location = GeoPoint( business.coordinates.latitude, business.coordinates.longitude)
                )
            }

            restaurantList.forEach { restaurant ->
                restaurantCollection.document(restaurant.id).set(restaurant)
            }

            restaurantList // ✅ Return new restaurants
        } catch (e: Exception) {
            Log.e("RestaurantRepo", "Error fetching Yelp data: ${e.message}")
            emptyList()
        }
    }

}
