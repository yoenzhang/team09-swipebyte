package com.example.swipebyte.ui.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore



class SwipeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userSwipesCollection = db.collection("userSwipes")

    fun recordSwipe(userId: String, restaurantId: String, isLiked: Boolean) {
        val swipeData = hashMapOf(
            "userId" to userId,
            "restaurantId" to restaurantId,
            "action" to if (isLiked) 1 else -1,
            "timestamp" to FieldValue.serverTimestamp()
        )

        userSwipesCollection.add(swipeData)
            .addOnSuccessListener { Log.d("SwipeRepo", "Swipe recorded successfully") }
            .addOnFailureListener { Log.e("SwipeRepo", "Error recording swipe", it) }
    }

    // Fetch all swipes of a user
    fun getUserSwipes(userId: String, callback: (List<DocumentSnapshot>) -> Unit) {
        userSwipesCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { documents -> callback(documents.documents) }
    }
}
