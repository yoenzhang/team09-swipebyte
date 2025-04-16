package com.swipebyte.ui.db.repository

import android.util.Log
import com.swipebyte.ui.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository : UserRepository {
    private val TAG = "FirebaseUserRepository"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun saveUserData(displayName: String) {
        val user = auth.currentUser ?: run {
            Log.w(TAG, "No authenticated user found")
            return
        }

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
                    .addOnSuccessListener { Log.d(TAG, "New user data saved successfully!") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error saving new user data: ${e.message}") }
            } else {
                // Existing user, just update the fields we want to change
                userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "User data updated successfully!") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error updating user data: ${e.message}") }
            }
        }
    }

    override suspend fun getUserData(): Pair<String, Map<String, Any>>? {
        val user = auth.currentUser ?: return null

        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) doc.id to (doc.data ?: emptyMap()) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            null
        }
    }

    override fun updateUserLocation(latitude: Double, longitude: Double) {
        val user = auth.currentUser ?: run {
            Log.w(TAG, "No authenticated user found")
            return
        }

        val userRef = db.collection("users").document(user.uid)
        val geoPoint = GeoPoint(latitude, longitude)

        userRef.update("location", geoPoint)
            .addOnSuccessListener { Log.d(TAG, "User location updated!") }
            .addOnFailureListener { e -> Log.e(TAG, "Error updating location: ${e.message}") }
    }

    override suspend fun getUserLocation(): GeoPoint? {
        val user = auth.currentUser ?: return null

        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                val geoPoint = doc.getGeoPoint("location")
                geoPoint?.let {
                    Log.d(TAG, "User Location - Latitude: ${it.latitude}, Longitude: ${it.longitude}")
                }
                geoPoint
            } else {
                Log.d(TAG, "No location found for user.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user location", e)
            null
        }
    }

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: FirebaseUserRepository? = null

        fun getInstance(): FirebaseUserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseUserRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}