package com.example.swipebyte.ui.db.models

data class UserSwipe(
    val userId: String = "",
    val restaurantId: String = "",
    val action: Int = 0, // +1 for like, -1 for dislike
    val timestamp: Long = System.currentTimeMillis()
)