package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.ui.data.models.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyLikesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _likedRestaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val likedRestaurants: StateFlow<List<Restaurant>> = _likedRestaurants

    private val _timestampsMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val timestampsMap: StateFlow<Map<String, Long>> = _timestampsMap

    fun fetchUserSwipedRestaurants(userId: String) {
        viewModelScope.launch {
            try {
                val swipeDocs = db.collection("userSwipes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("action", 1)
                    .get()
                    .await()

                val tsMap = mutableMapOf<String, Long>()
                val restaurantIds = mutableSetOf<String>()

                for (doc in swipeDocs.documents) {
                    val restId = doc.getString("restaurantId") ?: continue
                    val ts = doc.getLong("timestamp") ?: continue
                    restaurantIds.add(restId)
                    tsMap[restId] = ts
                }

                val restaurantsList = mutableListOf<Restaurant>()
                for (restId in restaurantIds) {
                    val restDoc = db.collection("restaurants").document(restId).get().await()
                    if (restDoc.exists()) {
                        restDoc.toObject(Restaurant::class.java)?.let { restaurant ->
                            // If your Restaurant model doesn't have an 'id' field, you may need to add one,
                            // or store the Firestore doc ID in some property on 'restaurant' for reference.
                            restaurantsList.add(restaurant)
                        }
                    }
                }

                _timestampsMap.value = tsMap
                _likedRestaurants.value = restaurantsList
            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching swiped restaurants: ", e)
            }
        }
    }
}
