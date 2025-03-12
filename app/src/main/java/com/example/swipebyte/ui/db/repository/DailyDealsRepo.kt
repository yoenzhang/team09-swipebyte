package com.example.swipebyte.ui.db.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

// Data class representing the deal details for a given day.
data class DayDeals(
    val description: List<String> = emptyList()
)

// Data class representing the overall Daily Deal document.
data class DailyDeal(
    val name: String = "",
    val address: String = "",
    val url: String = "",
    val images: List<String> = emptyList(),
    // A map where each key is a day (e.g., "Sunday") and the value is the deals info.
    val deals: Map<String, DayDeals> = emptyMap()
)

class DailyDealsRepository {
    // Initialize Firestore and optionally set settings.
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    private val dealsCollection = db.collection("dealsOfTheDay")

    // List of full day names used in your Firebase documents.
    private val daysOfWeek = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    // Fetch all daily deals from the Firestore "dealsOfTheDay" collection.
    suspend fun getDailyDeals(): List<DailyDeal> = withContext(Dispatchers.IO) {
        try {
            val snapshot = dealsCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                mapDocumentToDailyDeal(doc)
            }
        } catch (e: Exception) {
            Log.e("DailyDealsRepo", "Error fetching daily deals: ${e.message}")
            emptyList()
        }
    }

    // Helper function to map a Firestore document to a DailyDeal instance.
    private fun mapDocumentToDailyDeal(doc: com.google.firebase.firestore.DocumentSnapshot): DailyDeal? {
        return try {
            // Get the common fields.
            val name = doc.getString("name") ?: ""
            val address = doc.getString("address") ?: ""
            val url = doc.getString("url") ?: ""
            val images = doc.get("images") as? List<*> ?: emptyList<Any>()
            val imagesList = images.filterIsInstance<String>()

            // Map each day key (Sunday, Monday, etc.) to a DayDeals instance.
            val dealsMap = mutableMapOf<String, DayDeals>()
            for (day in daysOfWeek) {
                val dayData = doc.get(day) as? Map<*, *>
                val description = (dayData?.get("description") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                dealsMap[day] = DayDeals(description = description)
            }

            DailyDeal(
                name = name,
                address = address,
                url = url,
                images = imagesList,
                deals = dealsMap
            )
        } catch (e: Exception) {
            Log.e("DailyDealsRepo", "Error mapping document to DailyDeal: ${e.message}")
            null
        }
    }
}
