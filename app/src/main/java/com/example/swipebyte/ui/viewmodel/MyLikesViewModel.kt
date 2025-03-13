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

    /**
     * Fetch all restaurants that a given [userId] has swiped on (e.g., action=1 for 'like').
     * The userSwipes docs must contain fields:
     *   - userId (String)
     *   - restaurantId (String)
     *   - action (Int) [optional if you only want likes, use .whereEqualTo("action", 1)]
     */
    fun fetchUserSwipedRestaurants(userId: String) {
        viewModelScope.launch {
            try {
                // 1) Query userSwipes for docs matching the current userId
                //    and optionally filter to action=1 if you only want 'likes'.
                val swipeDocs = db.collection("userSwipes")
                    .whereEqualTo("userId", userId)
                    // .whereEqualTo("action", 1) // Uncomment if you only want liked swipes
                    .get()
                    .await()

                // 2) Extract restaurant IDs from these documents
                val restaurantIds = swipeDocs.documents.mapNotNull { doc ->
                    doc.getString("restaurantId")
                }.distinct()

                // 3) Fetch each restaurant from the "restaurants" collection by its doc ID
                val restaurantsList = mutableListOf<Restaurant>()
                for (id in restaurantIds) {
                    val restaurantDoc = db.collection("restaurants")
                        .document(id)
                        .get()
                        .await()

                    if (restaurantDoc.exists()) {
                        restaurantDoc.toObject(Restaurant::class.java)?.let { restaurant ->
                            restaurantsList.add(restaurant)
                        }
                    } else {
                        Log.w("MyLikesViewModel", "No matching doc in 'restaurants' for ID: $id")
                    }
                }

                // 4) Update our StateFlow with the results
                _likedRestaurants.value = restaurantsList

            } catch (e: Exception) {
                Log.e("MyLikesViewModel", "Error fetching swiped restaurants: ", e)
            }
        }
    }
}
