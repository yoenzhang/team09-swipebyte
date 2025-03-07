package com.example.swipebyte.ui.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

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

                val userData = mapOf(
                    "displayName" to effectiveDisplayName,
                    "email" to (user.email ?: ""),
                    "createdAt" to System.currentTimeMillis(),
                    "lastLogin" to System.currentTimeMillis(),
                    "cuisinePreferences" to emptyList<String>(),
                    "location" to GeoPoint(0.0, 0.0)
                )

                userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener { println("User data saved successfully!") }
                    .addOnFailureListener { e -> println("Error saving user data: ${e.message}") }
            } ?: println("No authenticated user found")
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
