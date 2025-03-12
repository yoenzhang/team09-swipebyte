package com.example.swipebyte.ui.db.repository

import android.util.Log
import com.example.swipebyte.ui.db.models.FriendRequest
import com.example.swipebyte.ui.db.models.Friendship
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
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
    fun sendFriendRequest(senderId: String, receiverId: String, onResult: (String) -> Unit) {
        db.collection("friendRequests")
            .where(
                Filter.or(
                    Filter.and(
                        Filter.equalTo("senderId", senderId),
                        Filter.equalTo("receiverId", receiverId)
                    ),
                    Filter.and(
                        Filter.equalTo("senderId", receiverId),
                        Filter.equalTo("receiverId", senderId)
                    )
                )
            )
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No existing request, proceed to send a new request
                    val request = hashMapOf(
                        "senderId" to senderId,
                        "receiverId" to receiverId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    db.collection("friendRequests")
                        .add(request)
                        .addOnSuccessListener {
                            Log.d("FriendRepo", "Friend request sent successfully.")
                            onResult("Friend request sent successfully.")
                        }
                        .addOnFailureListener {
                            Log.e("FriendRepo", "Failed to send friend request.")
                            onResult("Failed to send friend request. Please try again.")
                        }
                } else {
                    val existingRequest = documents.first()
                    val existingSender = existingRequest.getString("senderId")
                    val existingReceiver = existingRequest.getString("receiverId")

                    if (existingSender == receiverId && existingReceiver == senderId) {
                        // Reverse request exists, accept it instead of sending a new request
                        acceptFriendRequest(existingRequest.id, receiverId, senderId) { success ->
                            if (success) {
                                Log.d("FriendRepo", "Friend request accepted.")
                                onResult("Friend request accepted. You are now friends!")
                            } else {
                                Log.e("FriendRepo", "Failed to accept friend request.")
                                onResult("Failed to accept friend request. Please try again.")
                            }
                        }
                    } else {
                        // A request already exists in the same direction
                        Log.e("FriendRepo", "Friend request already exists between $senderId and $receiverId.")
                        onResult("Friend request already exists.")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FriendRepo", "Error checking existing requests", exception)
                onResult("Error checking existing friend requests. Please try again.")
            }
    }




    // ✅ Accept a friend request
    fun acceptFriendRequest(requestId: String, senderId: String, receiverId: String, onResult: (Boolean) -> Unit) {
        val friendshipData = mapOf(
            "user1" to senderId,
            "user2" to receiverId,
        )

        db.runBatch { batch ->
            // Just add the friendship to the "friends" collection
            batch.set(db.collection("friends").document(), friendshipData)  // Auto-generates document ID

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

    // Get list of user's friends with their display names
    fun loadFriendsList(userId: String, onResult: (List<Pair<String, String>>) -> Unit) {
        Log.d("FriendRepo", "Loading friends list for user: $userId")

        db.collection("friends")
            .where(
                Filter.or(
                    Filter.equalTo("user1", userId),
                    Filter.equalTo("user2", userId)
                )
            )
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FriendRepo", "Retrieved ${documents.size()} friendships")

                if (documents.isEmpty) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                // Extract the friend IDs (the other user in each friendship)
                val friendships = documents.map { doc ->
                    val user1 = doc.getString("user1") ?: ""
                    val user2 = doc.getString("user2") ?: ""
                    // If user1 is the current user, return user2 as the friend, otherwise return user1
                    if (user1 == userId) user2 else user1
                }

                // Fetch friend names from the users collection
                fetchFriendNames(friendships, onResult)
            }
            .addOnFailureListener { exception ->
                Log.e("FriendRepo", "Failed to load friends list", exception)
                onResult(emptyList())
            }
    }

    // Fetch friend names from "users" collection
    private fun fetchFriendNames(friendIds: List<String>, onResult: (List<Pair<String, String>>) -> Unit) {
        val results = mutableListOf<Pair<String, String>>()
        var remainingFriends = friendIds.size

        if (friendIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        for (friendId in friendIds) {
            db.collection("users").document(friendId).get()
                .addOnSuccessListener { document ->
                    val displayName = document.getString("displayName") ?: "Unknown"
                    results.add(friendId to displayName)

                    // When all names are fetched, return the results
                    if (--remainingFriends == 0) {
                        // Sort by display name for better user experience
                        onResult(results.sortedBy { it.second })
                    }
                }
                .addOnFailureListener {
                    Log.e("FriendRepo", "Failed to fetch name for $friendId")

                    // Default to "Unknown" if fetching fails
                    results.add(friendId to "Unknown")

                    if (--remainingFriends == 0) {
                        onResult(results.sortedBy { it.second })
                    }
                }
        }
    }


}
