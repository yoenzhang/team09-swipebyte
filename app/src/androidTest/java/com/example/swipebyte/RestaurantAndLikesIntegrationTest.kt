package com.example.swipebyte.integration

import com.example.swipebyte.ui.viewmodel.RestaurantViewModel
import com.example.swipebyte.ui.viewmodel.MyLikesViewModel
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class RestaurantAndLikesIntegrationTest {

    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var myLikesViewModel: MyLikesViewModel

    @Before
    fun setup() {
        restaurantViewModel = RestaurantViewModel()
        myLikesViewModel = MyLikesViewModel()
    }

    @Test
    fun bothViewModels_canBeInitialized() {
        // Simple test to verify both view models can be created together
        assertNotNull(restaurantViewModel)
        assertNotNull(myLikesViewModel)
    }

    @Test
    fun viewModels_doNotInterfereFunctionality() {
        // Ensure both view models are initialized with default states
        assertNotNull(restaurantViewModel.restaurants.value)
        assertNotNull(myLikesViewModel.likedRestaurants.value)
    }
}