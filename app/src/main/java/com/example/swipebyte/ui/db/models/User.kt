package com.example.swipebyte.ui.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class User(
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val cuisinePreferences: List<String> = emptyList(),
    val pricePreferences: List<String> = emptyList(),
    val location: GeoPoint = GeoPoint(0.0, 0.0)
)

class UserQueryable {
    companion object {
        fun saveUserDataToFirestore(displayName: String = "") {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser

            user?.let {
                val userRef = db.collection("users").document(user.uid)

                // Use provided displayName if not empty, otherwise try to get from Auth
                val effectiveDisplayName = if (displayName.isNotEmpty()) {
                    displayName
                } else {
                    user.displayName ?: ""
                }

                // Only update fields we actually want to change
                val userData = mapOf(
                    "displayName" to effectiveDisplayName,
                    "email" to (user.email ?: ""),
                    "lastLogin" to System.currentTimeMillis()
                )

                // For new users, add these default fields
                userRef.get().addOnSuccessListener { document ->
                    if (!document.exists()) {
                        // This is a new user, set default values for preferences
                        val newUserData = userData + mapOf(
                            "createdAt" to System.currentTimeMillis(),
                            "cuisinePreferences" to emptyList<String>(),
                            "pricePreferences" to emptyList<String>(),
                            "location" to GeoPoint(0.0, 0.0)
                        )
                        userRef.set(newUserData)
                            .addOnSuccessListener { println("New user data saved successfully!") }
                            .addOnFailureListener { e -> println("Error saving new user data: ${e.message}") }
                    } else {
                        // Existing user, just update the fields we want to change
                        userRef.set(userData, SetOptions.merge())
                            .addOnSuccessListener { println("User data updated successfully!") }
                            .addOnFailureListener { e -> println("Error updating user data: ${e.message}") }
                    }
                }
            } ?: println("No authenticated user found")
        }

        suspend fun getUserData(): Pair<String, Map<String, Any>>? {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser

            return user?.let {
                val doc = db.collection("users").document(user.uid).get().await()
                if (doc.exists()) doc.id to (doc.data ?: emptyMap()) else null
            }
        }

        fun updateUserLocation(latitude: Double, longitude: Double) {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser

            user?.let {
                val userRef = db.collection("users").document(user.uid)
                val geoPoint = GeoPoint(latitude, longitude)

                userRef.update("location", geoPoint)
                    .addOnSuccessListener { println("User location updated!") }
                    .addOnFailureListener { println("Error updating location: ${it.message}") }
            } ?: println("No authenticated user found")
        }

        suspend fun getUserLocation(): GeoPoint? {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser

            return user?.let {
                val doc = db.collection("users").document(user.uid).get().await()
                if (doc.exists()) {
                    val geoPoint = doc.getGeoPoint("location")
                    geoPoint?.let {
                        println("User Location - Latitude: ${it.latitude}, Longitude: ${it.longitude}")
                    }
                    geoPoint
                } else {
                    println("No location found for user.")
                    null
                }
            }
        }
    }
}
