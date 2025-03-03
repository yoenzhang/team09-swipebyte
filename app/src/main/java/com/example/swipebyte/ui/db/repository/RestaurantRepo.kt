package com.example.swipebyte.ui.db.repository

import com.example.swipebyte.ui.db.models.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class RestaurantRepository {

    private val db = FirebaseFirestore.getInstance()

    // Fetch nearby restaurants (using GeoPoint and distance calculation)
    suspend fun getNearbyRestaurants(latitude: Double, longitude: Double): List<Restaurant> {
        val restaurants = mutableListOf<Restaurant>()

        val querySnapshot = db.collection("restaurants")
            .get()
            .await()

        for (document in querySnapshot.documents) {
            val location = document.getGeoPoint("location")
            location?.let {
                val restaurantLatitude = it.latitude
                val restaurantLongitude = it.longitude
                val distance = calculateDistance(latitude, longitude, restaurantLatitude, restaurantLongitude)

                if (distance <= 5) { // 5 km radius for example
                    val restaurant = document.toObject(Restaurant::class.java)
                    restaurant?.let { restaurants.add(it) }
                }
            }
        }

        return restaurants
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Haversine formula for distance calculation
        val R = 6371  // Earth's radius in kilometers
        val latDiff = Math.toRadians(lat2 - lat1)
        val lonDiff = Math.toRadians(lon2 - lon1)
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c  // Returns the distance in kilometers
    }
}