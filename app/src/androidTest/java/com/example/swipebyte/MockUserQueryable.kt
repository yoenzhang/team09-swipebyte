package com.example.swipebyte.ui.data.models

import com.google.firebase.firestore.GeoPoint

/**
 * Mock implementation of UserQueryable for testing
 * This will let us bypass Firestore calls during tests
 */
class MockUserQueryable {
    companion object {
        var isTestMode = false

        // Mock save user data
        fun saveUserDataToFirestore(displayName: String = "") {
            if (isTestMode) {
                println("TEST MODE: saveUserDataToFirestore called with displayName: $displayName")
                return
            }

            // Call the real implementation when not in test mode
            UserQueryable.saveUserDataToFirestore(displayName)
        }

        // Mock get user data
        suspend fun getUserData(): Pair<String, Map<String, Any>>? {
            if (isTestMode) {
                println("TEST MODE: getUserData called")
                // Return mock data
                return "test-user-id" to mapOf(
                    "displayName" to "Test User",
                    "email" to "user@gmail.com"
                )
            }

            // Call the real implementation when not in test mode
            return UserQueryable.getUserData()
        }

        // Mock update user location
        fun updateUserLocation(latitude: Double, longitude: Double) {
            if (isTestMode) {
                println("TEST MODE: updateUserLocation called with lat: $latitude, lng: $longitude")
                return
            }

            // Call the real implementation when not in test mode
            UserQueryable.updateUserLocation(latitude, longitude)
        }

        // Mock get user location
        suspend fun getUserLocation(): GeoPoint? {
            if (isTestMode) {
                println("TEST MODE: getUserLocation called")
                // Return mock location
                return GeoPoint(43.6532, -79.3832) // Toronto coordinates
            }

            // Call the real implementation when not in test mode
            return UserQueryable.getUserLocation()
        }
    }
}