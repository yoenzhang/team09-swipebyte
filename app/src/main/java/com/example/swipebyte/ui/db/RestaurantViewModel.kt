package com.example.swipebyte.ui.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import kotlinx.coroutines.launch

class RestaurantViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    // Restaurants data
    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error handling
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadRestaurants() {
        // Reset previous error state
        _error.value = null

        // Set loading to true before starting the operation
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = repository.getRestaurants()
                _restaurants.value = data

                // If data is empty, set an error message
                if (data.isEmpty()) {
                    _error.value = "No restaurants found"
                }
            } catch (e: Exception) {
                // Handle any exceptions during restaurant loading
                _error.value = e.localizedMessage ?: "An unknown error occurred"
                _restaurants.value = emptyList()
            } finally {
                // Ensure loading state is set to false regardless of success or failure
                _isLoading.value = false
            }
        }
    }

    // Optional: Method to retry loading restaurants
    fun retryLoadRestaurants() {
        loadRestaurants()
    }
}