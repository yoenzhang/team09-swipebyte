package com.swipebyte.model

import com.swipebyte.ui.data.models.Restaurant
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestaurantTest {
    private lateinit var restaurant: Restaurant
    private lateinit var restaurantNoOptionalFields: Restaurant

    @Before
    fun setup() {
        // Create a fully populated Restaurant
        restaurant = Restaurant(
            id = "123",
            name = "Test Restaurant",
            address = "123 Main St",
            phone = "555-123-4567",
            imageUrls = listOf("https://example.com/image.jpg", "https://example.com/image2.jpg"),
            yelpRating = 4.5f,
            priceRange = "$$",
            cuisineType = listOf("Italian", "Pizza"),
            location = GeoPoint(43.6532, -79.3832),
            distance = 1.5,
            url = "https://example.com/restaurant",
            hours = emptyList(),
            voteCount = 10
        )

        // Create a Restaurant with minimal required fields
        restaurantNoOptionalFields = Restaurant(
            id = "456",
            name = "Minimal Restaurant",
            address = null.toString(),
            phone = null.toString(),
            imageUrls = emptyList(),
            yelpRating = 0.0f,
            priceRange = null,
            cuisineType = emptyList(),
            location =  GeoPoint(0.0, 0.0),
            distance = 0.0,
            url = null.toString(),
            hours = null,
            voteCount = 0
        )
    }

    @Test
    fun restaurant_hasCorrectProperties() {
        assertEquals("123", restaurant.id)
        assertEquals("Test Restaurant", restaurant.name)
        assertEquals("123 Main St", restaurant.address)
        assertEquals("555-123-4567", restaurant.phone)
        assertEquals("https://example.com/image.jpg", restaurant.imageUrls.first())
        assertEquals(4.5f, restaurant.yelpRating)
        assertEquals("$$", restaurant.priceRange)
        assertEquals(listOf("Italian", "Pizza"), restaurant.cuisineType)

        // For latitude and longitude (double values)
        assertEquals(43.6532, restaurant.location.latitude, 0.0001)
        assertEquals(-79.3832, restaurant.location.longitude, 0.0001)


        assertEquals("https://example.com/restaurant", restaurant.url)
        assertEquals(10, restaurant.voteCount)
    }

    @Test
    fun restaurant_withMultipleImages_containsAllImages() {
        assertEquals(2, restaurant.imageUrls.size)
        assertEquals("https://example.com/image.jpg", restaurant.imageUrls[0])
        assertEquals("https://example.com/image2.jpg", restaurant.imageUrls[1])
    }

    @Test
    fun restaurant_withMultipleCuisines_containsAllCuisines() {
        assertEquals(2, restaurant.cuisineType.size)
        assertEquals("Italian", restaurant.cuisineType[0])
        assertEquals("Pizza", restaurant.cuisineType[1])
    }

    @Test
    fun restaurant_withNullOptionalFields_hasDefaultValuesOrNull() {
        assertEquals("456", restaurantNoOptionalFields.id)
        assertEquals("Minimal Restaurant", restaurantNoOptionalFields.name)
        assertTrue(restaurantNoOptionalFields.imageUrls.isEmpty())
        assertEquals(0.0f, restaurantNoOptionalFields.yelpRating)
        assertTrue(restaurantNoOptionalFields.cuisineType.isEmpty())
    }

    @Test
    fun restaurant_idAndName_areRequired() {
        assertNotNull(restaurant.id)
        assertNotNull(restaurant.name)
        assertNotNull(restaurantNoOptionalFields.id)
        assertNotNull(restaurantNoOptionalFields.name)
    }
}