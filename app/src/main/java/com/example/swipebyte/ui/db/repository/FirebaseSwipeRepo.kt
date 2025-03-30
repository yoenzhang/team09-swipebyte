package com.example.swipebyte.ui.db.repository

import android.util.Log
import com.example.swipebyte.ui.data.models.UserSwipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSwipeRepository : SwipeRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userSwipesCollection by lazy { db.collection("userSwipes") }

    override fun getUserSwipes(userId: String, callback: (List<DocumentSnapshot>) -> Unit) {
        userSwipesCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { documents -> callback(documents.documents) }
    }

    override suspend fun getRecentSwipes(timeframeMillis: Long): Map<String, Long> {
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

            recentSwipes
        } catch (e: Exception) {
            Log.e("FirebaseSwipeRepository", "Error fetching recent swipes", e)
            emptyMap()
        }
    }

    override suspend fun recordSwipe(restaurantId: String, restaurantName: String, isLiked: Boolean) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return

        // Retrieve Firestore user data from UserQueryable.getUserData()
        val userData = FirebaseUserRepository.getInstance().getUserData()
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

    override suspend fun deleteSwipe(restaurantId: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return false

        try {
            val userData = FirebaseUserRepository.getInstance().getUserData()
            val userId = userData?.first ?: currentUser.uid

            val documentId = "${userId}_$restaurantId"

            userSwipesCollection.document(documentId).delete().await()

            return true
        } catch (e: Exception) {
            Log.e("FirebaseSwipeRepository", "Error deleting swipe", e)
            return false
        }
    }

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: FirebaseSwipeRepository? = null

        fun getInstance(): FirebaseSwipeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseSwipeRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}