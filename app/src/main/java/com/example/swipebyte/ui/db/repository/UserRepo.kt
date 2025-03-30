package com.example.swipebyte.ui.db.repository

import com.google.firebase.firestore.GeoPoint

interface UserRepository {
    /**
     * Saves or updates user data in the database
     */
    fun saveUserData(displayName: String = "")

    /**
     * Retrieves user data from the database
     * @return Pair of user ID and user data map, or null if user not found
     */
    suspend fun getUserData(): Pair<String, Map<String, Any>>?

    /**
     * Updates the user's location in the database
     */
    fun updateUserLocation(latitude: Double, longitude: Double)

    /**
     * Retrieves the user's location from the database
     * @return GeoPoint containing the user's location, or null if not found
     */
    suspend fun getUserLocation(): GeoPoint?
}