package com.example.swipebyte.ui.db.models

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",    // ID of the user who sent the request
    val receiverId: String = "",  // ID of the user who received the request
    val timestamp: Long = 0L
)

data class Friendship(
    val user1Id: String = "",
    val user2Id: String = "",
    val status: String = "active"  // active when the friendship is ongoing
)
