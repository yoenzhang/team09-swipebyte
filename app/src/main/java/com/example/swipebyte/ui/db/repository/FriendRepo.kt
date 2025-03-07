package com.example.swipebyte.ui.db.repository

import android.util.Log
import com.example.swipebyte.ui.db.models.FriendRequest
import com.example.swipebyte.ui.db.models.Friendship
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FriendRepo {
    private val db = FirebaseFirestore.getInstance()


    fun getUserIdByEmail(email: String, onResult: (String?) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming document ID is the userId
                    val userId = documents.documents[0].id
                    onResult(userId)
                } else {
                    // If no user found
                    onResult(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FriendRepo", "Error fetching user by email", exception)
                onResult(null) // Return null if there's an error
            }
    }

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
    fun loadPendingRequests(userId: String, onResult: (List<Pair<FriendRequest, String>>) -> Unit) {
        Log.d("FriendRequests", "Loading pending requests for user: $userId")

        db.collection("friendRequests")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FriendRequests", "Document snapshot retrieved: ${documents.size()}")

                val requests = documents.map { doc ->
                    FriendRequest(
                        requestId = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                    )
                }

                // Fetch sender names
                fetchSenderNames(requests, onResult)
            }
            .addOnFailureListener { exception ->
                Log.e("FriendRequests", "Failed to load friend requests", exception)
                onResult(emptyList())
            }
    }

    // Fetch sender names from "users" collection
    private fun fetchSenderNames(requests: List<FriendRequest>, onResult: (List<Pair<FriendRequest, String>>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val results = mutableListOf<Pair<FriendRequest, String>>()
        var remainingRequests = requests.size

        if (requests.isEmpty()) {
            onResult(emptyList())
            return
        }

        for (request in requests) {
            db.collection("users").document(request.senderId).get()
                .addOnSuccessListener { document ->
                    val senderName = document.getString("displayName") ?: "Unknown"
                    results.add(request to senderName)

                    // When all names are fetched, return the results
                    if (--remainingRequests == 0) {
                        onResult(results)
                    }
                }
                .addOnFailureListener {
                    Log.e("FriendRequests", "Failed to fetch name for ${request.senderId}")

                    // Default to "Unknown" if fetching fails
                    results.add(request to "Unknown")

                    if (--remainingRequests == 0) {
                        onResult(results)
                    }
                }
        }
    }


}
