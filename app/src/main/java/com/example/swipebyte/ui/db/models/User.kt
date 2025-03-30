package com.example.swipebyte.ui.data.models

import com.google.firebase.firestore.GeoPoint

data class User(
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val cuisinePreferences: List<String> = emptyList(),
    val pricePreferences: List<String> = emptyList(),
    val location: GeoPoint = GeoPoint(0.0, 0.0)
)
