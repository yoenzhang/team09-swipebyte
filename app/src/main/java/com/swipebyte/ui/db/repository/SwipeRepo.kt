package com.swipebyte.ui.db.repository

import com.google.firebase.firestore.DocumentSnapshot

interface SwipeRepository {
    /**
     * Fetches all swipes for a specific user
     */
    fun getUserSwipes(userId: String, callback: (List<DocumentSnapshot>) -> Unit)

    /**
     * Gets swipes from the last specified timeframe in milliseconds
     * Default is 24 hours
     */
    suspend fun getRecentSwipes(timeframeMillis: Long = 24 * 60 * 60 * 1000): Map<String, Long>

    /**
     * Records a user's swipe action on a restaurant
     */
    suspend fun recordSwipe(restaurantId: String, restaurantName: String, isLiked: Boolean)

    /**
     * Deletes a user's swipe for a specific restaurant
     * Returns true if successful, false otherwise
     */
    suspend fun deleteSwipe(restaurantId: String): Boolean
}