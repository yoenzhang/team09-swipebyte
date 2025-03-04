package com.example.swipebyte.ui.data.models

import com.google.firebase.firestore.GeoPoint

data class YelpResponse(
    val businesses: List<YelpBusiness>
)

data class YelpBusiness(
    val id: String,
    val name: String,
    val rating: Double,
    val price: String?,
    val image_url: String?,
    val categories: List<YelpCategory>,
    val coordinates: GeoPoint
)


data class YelpCategory(
    val title: String
)
