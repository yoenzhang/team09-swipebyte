package com.example.swipebyte.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.SwipeQueryable
import com.example.swipebyte.ui.data.models.UserQueryable
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class RestaurantViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    private val _restaurants = MutableLiveData<List<Restaurant>>(emptyList())
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Load restaurants from repository with preferences and distance filtering
    fun loadRestaurants(context: Context, forceRefresh: Boolean = false) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Get the user's preferred search radius from SharedPreferences
                val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                val searchRadius = sharedPrefs.getFloat("location_radius", 5.0f).toDouble()

                // Get recently swiped restaurants (within the last 24 hours)
                val recentSwipes = SwipeQueryable.getRecentSwipes()
                Log.d("RestaurantViewModel", "Found ${recentSwipes.size} recently swiped restaurants")

                // Log each swiped restaurant ID for debugging
                recentSwipes.forEach { (id, timestamp) ->
                    Log.d("RestaurantViewModel", "Recently swiped: Restaurant ID $id at ${java.util.Date(timestamp)}")
                }

                // Get restaurants with distance filtering
                val result = repository.getRestaurants(
                    forceRefresh = forceRefresh,
                    useUserPreferences = true,
                    maxDistance = searchRadius,
                    context = context
                )

                Log.d("RestaurantViewModel", "Before filtering: ${result.size} restaurants")

                // Debug log each restaurant to check
                result.forEach { restaurant ->
                    val isSwiped = recentSwipes.containsKey(restaurant.id)
                    Log.d("RestaurantViewModel", "Restaurant ${restaurant.name} (ID: ${restaurant.id}) - Was swiped: $isSwiped")
                }

                // Filter out recently swiped restaurants
                val filteredResult = result.filter { restaurant ->
                    val keepRestaurant = !recentSwipes.containsKey(restaurant.id)
                    if (!keepRestaurant) {
                        Log.d("RestaurantViewModel", "Filtering out swiped restaurant: ${restaurant.name} (ID: ${restaurant.id})")
                    }
                    keepRestaurant
                }

                // Sort results by distance (closest first)
                val sortedResult = filteredResult.sortedBy { it.distance }

                Log.d("RestaurantViewModel", "After filtering: ${filteredResult.size} restaurants remain")

                _restaurants.value = sortedResult
                _isLoading.value = false

                // Log the first few restaurants and their distances for debugging
                sortedResult.take(5).forEach { restaurant ->
                    Log.d("RestaurantViewModel", "Final result - Restaurant: ${restaurant.name}, Distance: ${String.format("%.2f", restaurant.distance)} km")
                }

            } catch (e: Exception) {
                Log.e("RestaurantViewModel", "Error loading restaurants: ${e.message}", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // Function to calculate distances for all restaurants based on current user location
    // This can be called when location changes but we don't want to reload all data
    fun updateDistances(context: Context) {
        viewModelScope.launch {
            try {
                // Get current location
                val location = UserQueryable.getUserLocation()
                if (location != null) {
                    // Get current restaurants list
                    val currentList = _restaurants.value ?: emptyList()

                    if (currentList.isNotEmpty()) {
                        // Update distances and resort
                        val updatedList = currentList.map { restaurant ->
                            // Calculate distance using Haversine formula
                            val distance = calculateDistance(
                                location.latitude,
                                location.longitude,
                                restaurant.location.latitude,
                                restaurant.location.longitude
                            )
                            restaurant.copy(distance = distance)
                        }

                        // Get the user's preferred search radius
                        val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                        val searchRadius = sharedPrefs.getFloat("location_radius", 5.0f).toDouble()

                        // Filter by new radius and sort by distance
                        val filteredAndSorted = updatedList
                            .filter { it.distance <= searchRadius }
                            .sortedBy { it.distance }

                        _restaurants.value = filteredAndSorted

                        Log.d("RestaurantViewModel", "Updated distances for ${filteredAndSorted.size} restaurants")
                    }
                }
            } catch (e: Exception) {
                Log.e("RestaurantViewModel", "Error updating distances: ${e.message}")
            }
        }
    }

    // Reload restaurants with fresh data
    fun refreshRestaurants(context: Context) {
        Log.d("RestaurantViewModel", "Explicitly refreshing restaurants with forceRefresh=true")
        loadRestaurants(context, forceRefresh = true)
    }

    // Helper function to calculate distance between two points using Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}