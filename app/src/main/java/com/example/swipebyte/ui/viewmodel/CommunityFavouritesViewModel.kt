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

data class RestaurantVote(
    val restaurantId: String,
    val voteCount: Int
)

class CommunityFavouritesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _favorites = MutableStateFlow<List<Restaurant>>(emptyList())
    val favorites: StateFlow<List<Restaurant>> = _favorites

    fun fetchFavorites() {
        viewModelScope.launch {
            try {
                val result = db.collection("userSwipes").get().await() // Use await() here to suspend until Firestore finishes fetching

                val votesMap = mutableMapOf<String, Int>()

                for (document in result) {
                    val restaurantId = document.getString("restaurantId") ?: continue
                    val swipeValue = document.getLong("action")?.toInt() ?: 0

                    votesMap[restaurantId] = votesMap.getOrDefault(restaurantId, 0) + swipeValue
                }

                // Convert to list and sort by most liked
                val sortedVotes = votesMap.map { (id, count) -> RestaurantVote(id, count) }
                    .sortedByDescending { it.voteCount }

                // Fetch restaurant details based on sortedVotes
                val restaurantDetails = fetchRestaurantDetails(sortedVotes)

                // Update _favorites in the coroutine scope
                _favorites.value = restaurantDetails
            } catch (e: Exception) {
                // Handle any errors that might occur during the query or other operations
                Log.e("Error", "Failed to fetch user swipes or restaurant details", e)
            }
        }
    }

    private suspend fun fetchRestaurantDetails(voteList: List<RestaurantVote>): List<Restaurant> {
        val restaurantsCollection = db.collection("restaurants")

        val restaurantsList = mutableListOf<Restaurant>()

        voteList.forEach { vote ->
            val restaurantId = vote.restaurantId
            val doc = restaurantsCollection.document(restaurantId).get().await()

            if (doc.exists()) {
                val restaurant = doc.toObject(Restaurant::class.java)
                restaurant?.let {
                    restaurantsList.add(it)
                }
            }
        }

        return restaurantsList
    }
}
