package com.example.swipebyte.ui.db.models

data class User(
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val cuisinePreferences: List<String> = emptyList(),
    val priceRange: Int = 1
)
