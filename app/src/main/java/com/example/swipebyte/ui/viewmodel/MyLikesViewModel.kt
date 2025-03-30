package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.db.repository.FavouriteQueryable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val _favouritesMap = MutableStateFlow<Set<String>>(emptySet())
    val favouritesMap: StateFlow<Set<String>> = _favouritesMap
    private val _friendLikesMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val friendLikesMap: StateFlow<Map<String, List<String>>> = _friendLikesMap

    fun fetchUserSwipedRestaurants(userId: String) {
        _isLoading.value = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val likedRestList = mutableListOf<Restaurant>()
                val timestamps = mutableMapOf<String, Long>()
                
                // Get user swipes
                val swipes = db.collection("userSwipes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("action", 1) // Only likes (1)
                    .get()
                    .await()

                for (document in swipes.documents) {
                    val restaurantId = document.getString("restaurantId") ?: continue
                    val timestamp = document.getLong("timestamp") ?: 0L
                    
                    // Save timestamp data
                    timestamps[restaurantId] = timestamp
                    
                    // Get restaurant details
                    try {
                        val restDoc = db.collection("restaurants").document(restaurantId).get().await()
                        if (restDoc.exists()) {
                            val restaurant = restDoc.toObject(Restaurant::class.java)
                            restaurant?.let { 
                                it.id = restaurantId
                                likedRestList.add(it) 
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                _likedRestaurants.value = likedRestList
                _timestampsMap.value = timestamps
                
                checkFavouritesStatus(likedRestList.map { it.id ?: "" })
                
            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching swiped restaurants: ", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkFavouritesStatus(restaurantIds: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favouriteIds = mutableSetOf<String>()
                
                for (id in restaurantIds) {
                    if (id.isNotEmpty()) {
                        val isInFavourites = FavouriteQueryable.isInFavourites(id)
                        if (isInFavourites) {
                            favouriteIds.add(id)
                        }
                    }
                }
                
                _favouritesMap.value = favouriteIds
                
            } catch (e: Exception) {
            }
        }
    }

    // Public method to refresh favorites status without reloading all restaurants
    fun refreshFavouritesStatus() {
        val currentRestaurantIds = _likedRestaurants.value.mapNotNull { it.id }
        checkFavouritesStatus(currentRestaurantIds)
    }

    fun fetchFriendLikes(friendIds: List<String>) {
        if (friendIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val friendLikesResult = mutableMapOf<String, MutableList<String>>()

                for (friendId in friendIds) {
                    val swipes = db.collection("userSwipes")
                        .whereEqualTo("userId", friendId)
                        .whereEqualTo("action", 1) // Only likes (1)
                        .get()
                        .await()

                    for (document in swipes.documents) {
                        val restaurantId = document.getString("restaurantId") ?: continue
                        val displayName = document.getString("displayName") ?: continue
                        
                        val likesList = friendLikesResult.getOrPut(restaurantId) { mutableListOf() }
                        likesList.add(displayName)
                    }
                }

                _friendLikesMap.value = friendLikesResult
            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching friend likes: ", e)
                _friendLikesMap.value = emptyMap()
            }
        }
    }
}
