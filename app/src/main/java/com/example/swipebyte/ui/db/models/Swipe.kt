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
    val timestamp: Long = System.currentTimeMillis(), // query this table for user favourites
    val displayName: String
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

        suspend fun recordSwipe(restaurantId: String, restaurantName: String, isLiked: Boolean) {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return

            // Retrieve Firestore user data from UserQueryable.getUserData()
            val userData = UserQueryable.getUserData()
            val userId = userData?.first ?: currentUser.uid

            // username is set to the user's email
            val username = currentUser.email ?: (userData?.second?.get("email")?.toString() ?: "")

            // For displayName, prefer FirebaseAuth.currentUser.displayName; if not available, use Firestore data.
            val displayName = if (!currentUser.displayName.isNullOrEmpty()) {
                currentUser.displayName!!
            } else {
                userData?.second?.get("displayName")?.toString() ?: ""
            }

            val swipeData = UserSwipe(
                userId = userId,
                username = username,
                displayName = displayName,
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                action = if (isLiked) 1 else -1,
                timestamp = System.currentTimeMillis()
            )

            val documentId = "${userId}_$restaurantId"
            userSwipesCollection.document(documentId).set(swipeData).await()
        }

        suspend fun deleteSwipe(restaurantId: String): Boolean {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return false

            try {
                val userData = UserQueryable.getUserData()
                val userId = userData?.first ?: currentUser.uid

                val documentId = "${userId}_$restaurantId"

                userSwipesCollection.document(documentId).delete().await()

                Log.d("SwipeRepo", "Successfully deleted swipe for restaurant $restaurantId")
                return true
            } catch (e: Exception) {
                Log.e("SwipeRepo", "Error deleting swipe for restaurant $restaurantId", e)
                return false
            }
        }
    }
}