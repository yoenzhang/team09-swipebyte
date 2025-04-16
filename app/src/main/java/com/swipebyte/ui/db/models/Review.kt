package com.swipebyte.ui.db.models

data class Review(
    val restaurant: String = "", // POPULATE THIS WITH RESTAURANT IDS
    val rating: Float = 0f,
    val description: String = "",
    val date: Long = System.currentTimeMillis()
)
