package com.example.swipebyte.ui.data.models

import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Restaurant(
    val name: String = "",
    val cuisineType: List<String> = emptyList(),
    val phone: String = "",
    val address: String = "",
    val priceRange: String? = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val imageUrls: List<String> = emptyList(),
    val id: String = "",
    val averageRating: Float = 0f,
    val userRating: Float = 0f,
    val yelpRating: Float = 0f,
    var distance: Double = 0.0,
    val url: String = "",
    val hours: List<YelpHours>? = emptyList()

)

class RestaurantQueryable {
    companion object {
        private val restaurantList : List<Restaurant> = emptyList()

        // Fetch nearby restaurants (using GeoPoint and distance calculation)
        suspend fun filterNearbyRestaurants(allRestaurants: List<Restaurant>): List<Restaurant> {
            val restaurants = mutableListOf<Restaurant>()

            val curLocation = UserQueryable.getUserLocation()
            val latitude = curLocation?.latitude ?: 0.0
            val longitude = curLocation?.longitude ?: 0.0

            for (restaurant in allRestaurants) {
                val location = restaurant.location
                location.let {
                    val restaurantLatitude = it.latitude
                    val restaurantLongitude = it.longitude
                    val distance = calculateDistance(latitude, longitude, restaurantLatitude, restaurantLongitude)
                    restaurant.distance = distance
                    if (distance <= 5) { // 5 km radius for example, TODO: add this as param
                        restaurants.add(restaurant)
                    }
                }
            }
            val sortedRestaurants = restaurants.sortedBy { it.distance }
            return sortedRestaurants
        }

        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            // Haversine formula for distance calculation
            val r = 6371  // Earth's radius in kilometers
            val latDiff = Math.toRadians(lat2 - lat1)
            val lonDiff = Math.toRadians(lon2 - lon1)
            val a = sin(latDiff / 2) * sin(latDiff / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(lonDiff / 2) * sin(lonDiff / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c  // Returns the distance in kilometers
        }

        suspend fun insertData() {
            val db = FirebaseFirestore.getInstance()
            val restaurantCollection = db.collection("restaurants")

            try {
                // Fetch all existing users
                val existingRestaurants = restaurantCollection.get().await()
                    .documents.mapNotNull { it.getString("name") }.toSet()

                val newRestaurants = restaurantList.filter { it.name !in existingRestaurants }

                // Insert new users
                newRestaurants.forEach { restaurant ->
                    restaurantCollection.add(restaurant).await()
                    println("Inserted: ${restaurant.name}")
                }

                println("All restaurants processed!")
            } catch (e: Exception) {
                println("Error: $e")
            }
        }

        suspend fun fetchRestaurants(): List<Restaurant> {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("restaurants")
            val restaurants = mutableListOf<Restaurant>()

            return try {
                // Fetch all documents from the "restaurants" collection
                val querySnapshot = collectionRef.get().await()

                for (document in querySnapshot) {
                    val restaurant = document.toObject(Restaurant::class.java)
                    restaurant.let { restaurants.add(it) }
                }

                restaurants // Return the populated list
            } catch (e: Exception) {
                println("Error fetching restaurants: $e")
                emptyList() // Return an empty list if fetching fails
            }
        }
    }
}

