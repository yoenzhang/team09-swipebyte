package com.swipebyte.ui.db.models

data class Favourite(
    val userId: String = "",
    val restaurantId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)