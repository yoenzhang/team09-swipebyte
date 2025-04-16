package com.swipebyte.ui.data.models

data class UserSwipe(
    val userId: String = "",
    val username: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val action: Int = 0, // +1 for like, -1 for dislike
    val timestamp: Long = System.currentTimeMillis(), // query this table for user favourites
    val displayName: String
)
