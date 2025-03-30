package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.db.repository.FavouriteQueryable
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyLikesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Expose loading state to be collected by the UI (using Flow as in CommunityFavourites)
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

    // Listener registration for real-time updates
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Sets up a real-time snapshot listener to load the user's liked restaurants.
     * This mimics the loading mechanics in CommunityFavouritesView.
     */
    fun firebaseSwipeListenerForLikes(userId: String) {
        _isLoading.value = true
        // Remove any previous listener to avoid duplicates
        listenerRegistration?.remove()
        listenerRegistration = db.collection("userSwipes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("action", 1) // Only likes (action = 1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    _isLoading.value = false
                    Log.e("MyLikesViewModel", "Error listening to swipes: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val likedRestList = mutableListOf<Restaurant>()
                            val timestamps = mutableMapOf<String, Long>()
                            for (document in snapshots.documents) {
                                val restaurantId = document.getString("restaurantId") ?: continue
                                val timestamp = document.getLong("timestamp") ?: 0L
                                timestamps[restaurantId] = timestamp
                                try {
                                    val restDoc = db.collection("restaurants")
                                        .document(restaurantId)
                                        .get()
                                        .await()
                                    if (restDoc.exists()) {
                                        val restaurant = restDoc.toObject(Restaurant::class.java)
                                        restaurant?.let {
                                            it.id = restaurantId
                                            likedRestList.add(it)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MyLikesViewModel", "Error fetching restaurant details for $restaurantId: ${e.message}")
                                }
                            }
                            _likedRestaurants.value = likedRestList
                            _timestampsMap.value = timestamps
                            checkFavouritesStatus(likedRestList.map { it.id ?: "" })
                        } catch (e: Exception) {
                            Log.e("MyLikesViewModel", "Error processing swipes: ${e.message}")
                        } finally {
                            _isLoading.value = false
                        }
                    }
                }
            }
    }

    private fun checkFavouritesStatus(restaurantIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
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
                Log.e("MyLikesViewModel", "Error checking favourites: ${e.message}")
            }
        }
    }

    // Public method to refresh favourites status without reloading all restaurants
    fun refreshFavouritesStatus() {
        val currentRestaurantIds = _likedRestaurants.value.mapNotNull { it.id }
        checkFavouritesStatus(currentRestaurantIds)
    }

    fun fetchFriendLikes(friendIds: List<String>) {
        if (friendIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val friendLikesResult = mutableMapOf<String, MutableList<String>>()
                for (friendId in friendIds) {
                    val swipes = db.collection("userSwipes")
                        .whereEqualTo("userId", friendId)
                        .whereEqualTo("action", 1) // Only likes
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
                Log.e("MyLikesViewModel", "Error fetching friend likes: ${e.message}")
                _friendLikesMap.value = emptyMap()
            }
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}
