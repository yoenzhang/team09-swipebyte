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