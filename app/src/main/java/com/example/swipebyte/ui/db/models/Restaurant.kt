package com.example.swipebyte.ui.db.models

import com.google.firebase.firestore.GeoPoint

data class Restaurant(
    val name: String = "",
    val cuisineType: List<String> = emptyList(),
    val priceRange: Int = 1,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",
    val phoneNumber: String = "",
    val website: String = "",
    val imageUrls: List<String> = emptyList(),
    val yelpId: String = "",
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val userRating: Float = 0f
)
