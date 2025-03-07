package com.example.swipebyte.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import kotlinx.coroutines.launch

class RestaurantViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // The loadRestaurants now passes the forceRefresh parameter to the repository.
    fun loadRestaurants(forceRefresh: Boolean = false) {
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val data = repository.getRestaurants(forceRefresh)
                _restaurants.value = data

                if (data.isEmpty()) {
                    _error.value = "No restaurants found"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An unknown error occurred"
                _restaurants.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLoadRestaurants() {
        loadRestaurants(forceRefresh = true)
    }
}
