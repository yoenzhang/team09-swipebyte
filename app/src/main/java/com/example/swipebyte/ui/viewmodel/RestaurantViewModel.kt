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
import com.example.swipebyte.ui.db.observer.*
import kotlinx.coroutines.launch
import java.util.Date

class RestaurantViewModel : ViewModel(), RestaurantObserver, PreferencesObserver {
    private val repository = RestaurantRepository()
    private val restaurantObservable = RestaurantDataObserver.getObservable()
    private val preferencesObservable = PreferencesDataObserver.getObservable()

    private val _restaurants = MutableLiveData<List<Restaurant>>(emptyList())
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Store last known context for refreshing when preferences change
    private var lastContext: Context? = null
    private var currentPreferences = UserPreferences()

    init {
        // Register as an observer for restaurants and preferences
        restaurantObservable.registerObserver(this)
        preferencesObservable.registerObserver(this)

        // Initialize current preferences
        currentPreferences = preferencesObservable.getPreferences()
    }

    override fun onCleared() {
        // Clean up by unregistering when ViewModel is destroyed
        restaurantObservable.unregisterObserver(this)
        preferencesObservable.unregisterObserver(this)
        super.onCleared()
    }

    // RestaurantObserver implementation
    override fun onRestaurantUpdate(data: List<Restaurant>) {
        _restaurants.value = data
        _isLoading.value = false
        Log.d("RestaurantViewModel", "Restaurant data updated with ${data.size} restaurants")
    }

    // PreferencesObserver implementation
    override fun onPreferencesUpdate(data: UserPreferences) {
        Log.d("RestaurantViewModel", "Preferences updated: cuisines=${data.cuisinePreferences.size}, " +
                "price=${data.pricePreferences.size}, radius=${data.locationRadius}")

        // Store the new preferences
        currentPreferences = data

        // Reload restaurants with new preferences if we have a context
        lastContext?.let {
            refreshRestaurants(it)
        }
    }

    // Load restaurants from repository with preferences and distance filtering
    fun loadRestaurants(context: Context, forceRefresh: Boolean = false) {
        _isLoading.value = true
        _error.value = null
        lastContext = context // Store for preference updates

        viewModelScope.launch {
            try {
                // Get the user's preferred search radius from current preferences
                val searchRadius = currentPreferences.locationRadius.toDouble()
                Log.d("RestaurantViewModel", "Loading restaurants with radius: $searchRadius km")

                // Get recently swiped restaurants (within the last 24 hours)
                val recentSwipes = SwipeQueryable.getRecentSwipes()
                Log.d("RestaurantViewModel", "Found ${recentSwipes.size} recently swiped restaurants")

                // Log each swiped restaurant ID for debugging
                recentSwipes.forEach { (id, timestamp) ->
                    Log.d("RestaurantViewModel", "Recently swiped: Restaurant ID $id at ${Date(timestamp)}")
                }

                // Get restaurants with distance filtering
                val result = repository.getRestaurants(
                    forceRefresh = forceRefresh,
                    useUserPreferences = true,
                    maxDistance = searchRadius,
                    context = context
                )

                Log.d("RestaurantViewModel", "Before filtering: ${result.size} restaurants")

                // Filter out recently swiped restaurants
                val filteredResult = result.filter { restaurant ->
                    val keepRestaurant = !recentSwipes.containsKey(restaurant.id)
                    if (!keepRestaurant) {
                        Log.d("RestaurantViewModel", "Filtering out swiped restaurant: ${restaurant.name} (ID: ${restaurant.id})")
                    }
                    keepRestaurant
                }

                // Apply cuisine filtering if set in preferences
                val cuisineFiltered = if (currentPreferences.cuisinePreferences.isNotEmpty()) {
                    filteredResult.filter { restaurant ->
                        // Check if any of the restaurant's cuisine types match any of the user's preferences
                        restaurant.cuisineType.any { cuisine ->
                            currentPreferences.cuisinePreferences.contains(cuisine)
                        }
                    }
                } else {
                    filteredResult
                }

                // Apply price range filtering if set in preferences
                val priceFiltered = if (currentPreferences.pricePreferences.isNotEmpty()) {
                    cuisineFiltered.filter { restaurant ->
                        currentPreferences.pricePreferences.contains(restaurant.priceRange)
                    }
                } else {
                    cuisineFiltered
                }

                // Sort results by distance (closest first)
                val sortedResult = priceFiltered.sortedBy { it.distance }

                Log.d("RestaurantViewModel", "After filtering: ${sortedResult.size} restaurants remain")

                // Update the observable (which will notify all observers including this ViewModel)
                restaurantObservable.updateRestaurants(sortedResult)

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
    fun updateDistances(context: Context) {
        lastContext = context // Store for preference updates

        viewModelScope.launch {
            try {
                // Get current location
                val location = UserQueryable.getUserLocation()
                if (location != null) {
                    // Get current restaurants list from the observable
                    val currentList = restaurantObservable.getRestaurants()

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
                        val searchRadius = currentPreferences.locationRadius.toDouble()

                        // Filter by new radius and sort by distance
                        val filteredAndSorted = updatedList
                            .filter { it.distance <= searchRadius }
                            .sortedBy { it.distance }

                        // Update the observable with new data
                        restaurantObservable.updateRestaurants(filteredAndSorted)

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