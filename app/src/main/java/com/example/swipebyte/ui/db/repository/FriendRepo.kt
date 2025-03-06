package com.example.swipebyte.ui.db.repository

import android.util.Log
import com.example.swipebyte.ui.db.models.FriendRequest
import com.example.swipebyte.ui.db.models.Friendship
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FriendRepo {
    private val db = FirebaseFirestore.getInstance()

    // ✅ Send a friend request
    fun sendFriendRequest(senderId: String, receiverId: String, onResult: (Boolean) -> Unit) {
        val request = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("friendRequests")
            .add(request)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ✅ Accept a friend request
    fun acceptFriendRequest(requestId: String, senderId: String, receiverId: String, onResult: (Boolean) -> Unit) {
        val userRef = db.collection("friends").document(receiverId)
        val friendRef = db.collection("friends").document(senderId)

        db.runBatch { batch ->
            // Add each user to the other's friend list
            batch.update(userRef, "friends", FieldValue.arrayUnion(senderId))
            batch.update(friendRef, "friends", FieldValue.arrayUnion(receiverId))

            // Delete the friend request
            batch.delete(db.collection("friendRequests").document(requestId))
        }.addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ✅ Decline a friend request
    fun declineFriendRequest(requestId: String, onResult: (Boolean) -> Unit) {
        db.collection("friendRequests").document(requestId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ✅ Load pending friend requests
    fun loadPendingRequests(userId: String, onResult: (List<FriendRequest>) -> Unit) {
        Log.d("FriendRequests", "Loading pending requests for user: $userId") // Log before calling Firestore

        db.collection("friendRequests")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order latest requests first
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FriendRequests", "Document snapshot retrieved: ${documents.size()}") // Log number of docs retrieved
                val requests = documents.map { doc ->
                    FriendRequest(
                        requestId = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                    )
                }
                Log.d("FriendRequests", "Loaded friend requests: $requests") // Log the requests
                onResult(requests)
            }
            .addOnFailureListener { exception ->
                Log.e("FriendRequests", "Failed to load friend requests", exception) // Log any error
                onResult(emptyList())
            }
    }


}
