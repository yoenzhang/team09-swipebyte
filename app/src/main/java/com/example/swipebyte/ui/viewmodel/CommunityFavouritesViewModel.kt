package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.RestaurantQueryable.Companion.calculateDistance
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RestaurantVote(
    val restaurantId: String,
    val voteCount: Int
)

class CommunityFavouritesViewModel() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _favorites = MutableStateFlow<List<Restaurant>>(emptyList())
    val favorites: StateFlow<List<Restaurant>> = _favorites

    private val _isLoading = MutableStateFlow(true) // Track loading state
    val isLoading: StateFlow<Boolean> = _isLoading

    private var userLocation: GeoPoint? = null
    private var listenerRegistration: ListenerRegistration? = null

    private val restaurantCache = mutableMapOf<String, Restaurant>() // Cache to reduce Firestore reads

    fun firebaseSwipeListener(userLocation: GeoPoint?) {
        this.userLocation = userLocation
        _isLoading.value = true // Start loading

        listenerRegistration?.remove() // Remove any previous listener to prevent duplicates

        listenerRegistration = db.collection("userSwipes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("CommunityFavourites", "Error listening for updates", error)
                    _isLoading.value = false // Stop loading on error
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val votesMap = mutableMapOf<String, Int>()

                    for (document in snapshots.documents) {
                        val restaurantId = document.getString("restaurantId") ?: continue
                        val swipeValue = document.getLong("action")?.toInt() ?: 0

                        votesMap[restaurantId] = votesMap.getOrDefault(restaurantId, 0) + swipeValue
                    }

                    val sortedVotes = votesMap.map { (id, count) -> RestaurantVote(id, count) }
                        .sortedByDescending { it.voteCount }

                    updateFavorites(sortedVotes)
                }
            }
    }

    private fun updateFavorites(voteList: List<RestaurantVote>) {
        viewModelScope.launch {
            val updatedRestaurants = voteList.mapNotNull { vote ->

                val cachedRestaurant = restaurantCache[vote.restaurantId]?.copy(voteCount = vote.voteCount)

                // Use cached restaurant, just update the vote count
                cachedRestaurant?.copy(
                    distance = calculateDistance(
                        userLocation?.latitude ?: 0.0,
                        userLocation?.longitude ?: 0.0,
                        cachedRestaurant.location.latitude,
                        cachedRestaurant.location.longitude
                    )
                )
                ?: // Fetch from Firestore if not in cache
                fetchUpdatedFavesFromFireStore(vote.restaurantId, vote.voteCount)
            }

            _favorites.value = updatedRestaurants
            _isLoading.value = false // Stop loading after data is set
        }
    }

    private suspend fun fetchUpdatedFavesFromFireStore(restaurantId: String, voteCount: Int): Restaurant? {
        return try {
            val doc = db.collection("restaurants").document(restaurantId).get().await()

            if (doc.exists()) {
                val restaurant = doc.toObject(Restaurant::class.java)?.copy(voteCount = voteCount)

                restaurant?.let {
                    restaurantCache[restaurantId] = it // Store in cache
                    it.copy(
                        distance = calculateDistance(
                            userLocation?.latitude ?: 0.0,
                            userLocation?.longitude ?: 0.0,
                            it.location.latitude,
                            it.location.longitude
                        )
                    )
                }
            } else null
        } catch (e: Exception) {
            Log.e("CommunityFavourites", "Error fetching restaurant details", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove() // Remove listener to avoid memory leaks
    }
}
