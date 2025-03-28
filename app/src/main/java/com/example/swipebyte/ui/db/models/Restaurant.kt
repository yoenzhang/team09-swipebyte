package com.example.swipebyte.ui.data.models

import android.content.Context
import com.google.firebase.firestore.GeoPoint
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
    var id: String = "",
    val averageRating: Float = 0f,
    val userRating: Float = 0f,
    val yelpRating: Float = 0f,
    var distance: Double = 0.0,
    val url: String = "",
    val hours: List<YelpHours>? = emptyList(),
    val voteCount: Int = 0
)


class RestaurantQueryable {
    companion object {
        private val restaurantList : List<Restaurant> = emptyList()

        suspend fun filterNearbyRestaurants(
            allRestaurants: List<Restaurant>,
            context: Context? = null
        ): List<Restaurant> {
            val restaurants = mutableListOf<Restaurant>()

            // Get user location
            val curLocation = UserQueryable.getUserLocation()
            val latitude = curLocation?.latitude ?: 0.0
            val longitude = curLocation?.longitude ?: 0.0

            // Get radius preference (default to 5 km if not set)
            val radiusInKm = context?.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                ?.getFloat("location_radius", 5.0f)?.toDouble()
                ?: 5.0

            val radiusInMeters = radiusInKm * 1000

            for (restaurant in allRestaurants) {
                val location = restaurant.location
                location.let {
                    val restaurantLatitude = it.latitude
                    val restaurantLongitude = it.longitude
                    val distance = calculateDistance(latitude, longitude, restaurantLatitude, restaurantLongitude)
                    restaurant.distance = distance
                    if (distance <= radiusInMeters) {
                        restaurants.add(restaurant)
                    }
                }
            }
            val sortedRestaurants = restaurants.sortedBy { it.distance }
            return sortedRestaurants
        }

        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371 
            val latDiff = Math.toRadians(lat2 - lat1)
            val lonDiff = Math.toRadians(lon2 - lon1)
            val a = sin(latDiff / 2) * sin(latDiff / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(lonDiff / 2) * sin(lonDiff / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c  // in km 
        }
    }
}