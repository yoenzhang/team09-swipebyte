package com.example.swipebyte.ui.data.models

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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

        suspend fun recordSwipe(restaurantId: String, restaurantName: String, isLiked: Boolean) {
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

            userSwipesCollection.document(documentId).set(swipeData)
                .addOnSuccessListener { Log.d("SwipeRepo", "Swipe recorded successfully") }
                .addOnFailureListener { Log.e("SwipeRepo", "Error recording swipe", it) }
        }
    }
}