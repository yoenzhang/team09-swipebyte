package com.example.swipebyte.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.RestaurantQueryable
import kotlinx.coroutines.launch

class RestaurantViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Updated to incorporate context for location-based filtering
    fun loadRestaurants(context: Context, forceRefresh: Boolean = false) {
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // First get all restaurants
                val allRestaurants = repository.getRestaurants(forceRefresh)

                // Then filter by location
                val filteredRestaurants = RestaurantQueryable.filterNearbyRestaurants(
                    allRestaurants,
                    context
                )

                _restaurants.value = filteredRestaurants

                if (filteredRestaurants.isEmpty()) {
                    _error.value = "No restaurants found nearby"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An unknown error occurred"
                _restaurants.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLoadRestaurants(context: Context) {
        loadRestaurants(context, forceRefresh = true)
    }
}