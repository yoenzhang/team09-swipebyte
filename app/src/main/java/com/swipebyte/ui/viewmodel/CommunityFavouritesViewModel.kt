package com.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipebyte.ui.data.models.Restaurant
import com.swipebyte.ui.db.observer.PreferencesDataObserver
import com.swipebyte.ui.db.observer.PreferencesObserver
import com.swipebyte.ui.db.observer.UserPreferences
import com.swipebyte.ui.db.utils.LocationUtils.calculateDistance
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RestaurantVote(val restaurantId: String, val voteCount: Int)

class CommunityFavouritesViewModel : ViewModel(), PreferencesObserver {
    private val db = FirebaseFirestore.getInstance()
    private val preferencesObservable = PreferencesDataObserver.getObservable()

    private var currentPreferences = UserPreferences()

    private val _favorites = MutableStateFlow<List<Restaurant>>(emptyList())
    val favorites: StateFlow<List<Restaurant>> = _favorites

    private val _isLoading = MutableStateFlow(true) // Track loading state
    val isLoading: StateFlow<Boolean> = _isLoading

    // New state to hold the current time filter; default "All Time"
    private val _timeFilter = MutableStateFlow("All Time")

    fun setTimeFilter(newFilter: String) {
        _timeFilter.value = newFilter
        // Recalculate favorites with the new time filter using the latest swipe snapshot
        recomputeFavorites()
    }

    private var userLocation: GeoPoint? = null
    private var listenerRegistration: ListenerRegistration? = null

    private val restaurantCache = mutableMapOf<String, Restaurant>() // Cache to reduce Firestore reads

    private var latestSwipeDocuments: List<DocumentSnapshot> = emptyList()

    init {
        // Register as an observer for preferences
        preferencesObservable.registerObserver(this)

        // Initialize current preferences
        currentPreferences = preferencesObservable.getPreferences()
    }

    // PreferencesObserver implementation
    override fun onPreferencesUpdate(data: UserPreferences) {
        Log.d("CommunityFavouritesViewModel", "Preferences updated: cuisines=${data.cuisinePreferences.size}, " +
                "price=${data.pricePreferences.size}, radius=${data.locationRadius}")

        // Check if location radius changed
        val radiusChanged = currentPreferences.locationRadius != data.locationRadius

        // Store the new preferences
        currentPreferences = data

        // Reload restaurants if radius changed
        if (radiusChanged) {
            recomputeFavorites()
        }
    }

    fun firebaseSwipeListener(userLocation: GeoPoint?) {
        this.userLocation = userLocation
        _isLoading.value = true // Start loading

        listenerRegistration?.remove() // Remove any previous listener to prevent duplicates

        listenerRegistration = db.collection("userSwipes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    _isLoading.value = false // Stop loading on error
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    latestSwipeDocuments = snapshots.documents
                    // Recompute favorites using the current time filter
                    recomputeFavorites()
                }
            }
    }

    private fun recomputeFavorites() {
        val currentTime = System.currentTimeMillis()
        val cutoff = currentTime - 24 * 60 * 60 * 1000

        val allTimeVotes = mutableMapOf<String, Int>()
        val last24HourVotes = mutableMapOf<String, Int>()

        for (document in latestSwipeDocuments) {
            val restaurantId = document.getString("restaurantId") ?: continue
            val swipeValue = document.getLong("action")?.toInt() ?: 0

            // Aggregate for all time
            allTimeVotes[restaurantId] = allTimeVotes.getOrDefault(restaurantId, 0) + swipeValue

            // Check timestamp for last 24 hours
            val timestamp = document.getLong("timestamp") ?: 0L
            if (timestamp >= cutoff) {
                last24HourVotes[restaurantId] = last24HourVotes.getOrDefault(restaurantId, 0) + swipeValue
            }
        }

        val selectedVotes = if (_timeFilter.value == "Last 24 hours") last24HourVotes else allTimeVotes

        // Create a sorted list of RestaurantVote objects
        val sortedVotes = selectedVotes.map { (id, count) -> RestaurantVote(id, count) }
            .sortedByDescending { it.voteCount }

        updateFavorites(sortedVotes)
    }

    private fun updateFavorites(voteList: List<RestaurantVote>) {
        viewModelScope.launch {
            val updatedRestaurants = voteList.mapNotNull { vote ->

                val cachedRestaurant = restaurantCache[vote.restaurantId]?.copy(voteCount = vote.voteCount)
                // Calculate distance
                val distance = cachedRestaurant?.let {
                    calculateDistance(
                        userLocation?.latitude ?: 0.0,
                        userLocation?.longitude ?: 0.0,
                        it.location.latitude,
                        it.location.longitude
                    )
                } ?: fetchUpdatedFavesFromFireStore(vote.restaurantId, vote.voteCount)?.distance

                // Only include restaurants within the radius
                if (distance != null && distance <= currentPreferences.locationRadius) {
                    cachedRestaurant?.copy(distance = distance)
                        ?: fetchUpdatedFavesFromFireStore(vote.restaurantId, vote.voteCount)?.copy(distance = distance)
                } else {
                    null // Exclude restaurants outside the radius
                }
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
        preferencesObservable.unregisterObserver(this)
        super.onCleared()
        listenerRegistration?.remove() // Remove listener to avoid memory leaks
    }
}
