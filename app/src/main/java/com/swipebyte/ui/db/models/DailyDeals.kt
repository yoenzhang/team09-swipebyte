package com.swipebyte.ui.db.models

data class DailyDeal(
    val restaurantId: String = "",
    val description: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val discountPercentage: Float = 0f,
    val dealText: String = ""
)

