package com.example.swipebyte.ui.data.models

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserSwipe(
    val userId: String = "",
    val username: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val action: Int = 0, // +1 for like, -1 for dislike
    val timestamp: Long = System.currentTimeMillis() // query this table for user favourites
)

class SwipeQueryable {
    companion object {
        // Lazy initialization to prevent storing Firebase instances in static fields
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
        private val userSwipesCollection by lazy { db.collection("userSwipes") }

        // Fetch all swipes of a user
        fun getUserSwipes(userId: String, callback: (List<DocumentSnapshot>) -> Unit) {
            userSwipesCollection.whereEqualTo("userId", userId).get()
                .addOnSuccessListener { documents -> callback(documents.documents) }
        }

        // New function: Get swipes from the last 24 hours
        suspend fun getRecentSwipes(timeframeMillis: Long = 24 * 60 * 60 * 1000): Map<String, Long> {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyMap()
            val currentTime = System.currentTimeMillis()
            val cutoffTime = currentTime - timeframeMillis

            return try {
                val swipesDocs = userSwipesCollection
                    .whereEqualTo("userId", currentUserId)
                    .whereGreaterThan("timestamp", cutoffTime)
                    .get()
                    .await()

                val recentSwipes = mutableMapOf<String, Long>()
                for (doc in swipesDocs.documents) {
                    val restaurantId = doc.getString("restaurantId") ?: continue
                    val timestamp = doc.getLong("timestamp") ?: continue
                    recentSwipes[restaurantId] = timestamp
                }

                Log.d("SwipeRepo", "Found ${recentSwipes.size} recent swipes within last ${timeframeMillis / (60 * 60 * 1000)} hours")
                recentSwipes
            } catch (e: Exception) {
                Log.e("SwipeRepo", "Error getting recent swipes", e)
                emptyMap()
            }
        }

        // Modified to be a suspend function that awaits completion
        suspend fun recordSwipe(restaurantId: String, restaurantName: String, isLiked: Boolean) {
            try {
                val userData = UserQueryable.getUserData()
                var userId = ""
                var userInfo : Map<String, Any> = emptyMap()
                if (userData != null) {
                    userId = userData.first // Firestore document ID
                    userInfo = userData.second // User data as Map<String, Any>
                }
                val swipeData = UserSwipe(
                    userId,
                    userInfo["email"].toString(),
                    restaurantId,
                    restaurantName,
                    if (isLiked) 1 else -1,
                    System.currentTimeMillis()
                )

                val documentId = "${userId}_$restaurantId" // Unique key per user-restaurant swipe

                // Use await() to make this synchronous
                userSwipesCollection.document(documentId).set(swipeData).await()

                Log.d("SwipeRepo", "Recording ${if (isLiked) "LIKE" else "DISLIKE"} swipe on restaurant $restaurantName ($restaurantId)")
            } catch (e: Exception) {
                Log.e("SwipeRepo", "Error recording swipe", e)
                throw e  // Rethrow to handle in the caller
            }
        }
    }
}