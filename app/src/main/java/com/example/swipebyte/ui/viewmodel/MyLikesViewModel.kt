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

    // Expose loading state to be collected by the UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _likedRestaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val likedRestaurants: StateFlow<List<Restaurant>> = _likedRestaurants

    private val _timestampsMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val timestampsMap: StateFlow<Map<String, Long>> = _timestampsMap

    // New state: map of restaurant id to list of friend display names who liked that restaurant
    private val _friendLikesMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val friendLikesMap: StateFlow<Map<String, List<String>>> = _friendLikesMap

    fun fetchUserSwipedRestaurants(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
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
                            restaurantsList.add(restaurant)
                        }
                    }
                }

                _timestampsMap.value = tsMap
                _likedRestaurants.value = restaurantsList
            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching swiped restaurants: ", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New function: fetch friend likes given a list of friend IDs.
    // It queries "userSwipes" for swipes with action 1 from friends and groups displayNames by restaurantId.
    fun fetchFriendLikes(friendIds: List<String>) {
        viewModelScope.launch {
            try {
                if (friendIds.isEmpty()) {
                    _friendLikesMap.value = emptyMap()
                    return@launch
                }
                // For simplicity, we assume friendIds size is small (Firestore's whereIn supports up to 10 items)
                val swipeQuery = db.collection("userSwipes")
                    .whereIn("userId", friendIds)
                    .whereEqualTo("action", 1)
                    .get()
                    .await()

                val friendLikesTemp = mutableMapOf<String, MutableList<String>>()
                for (doc in swipeQuery.documents) {
                    val restId = doc.getString("restaurantId") ?: continue
                    // We assume that when a friend swipes, their displayName is stored in the swipe record.
                    val friendDisplayName = doc.getString("displayName") ?: continue
                    if (!friendLikesTemp.containsKey(restId)) {
                        friendLikesTemp[restId] = mutableListOf()
                    }
                    friendLikesTemp[restId]?.add(friendDisplayName)
                }
                _friendLikesMap.value = friendLikesTemp
            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching friend likes: ", e)
                _friendLikesMap.value = emptyMap()
            }
        }
    }
}
