package com.example.swipebyte.ui.data.models

import androidx.compose.ui.text.LinkAnnotation
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
    val coordinates: GeoPoint,
    val url: String,
    val phone: String,
    val location: YelpLocation,
)


data class YelpLocation(
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val country: String?,
    val state: String?,
    val zip_code: String?
)



data class YelpCategory(
    val title: String
)
