package com.example.swipebyte.ui.db.models

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Favourite(
    val userId: String = "",
    val restaurantId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class FavouriteQueryable {
    companion object {
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
        private val favouritesCollection by lazy { db.collection("favourites") }

        suspend fun addToFavourites(restaurantId: String): Boolean {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return false
            
            try {
                val userId = currentUser.uid
                
                // Create document ID using userId and restaurantId
                val documentId = "${userId}_${restaurantId}"
                
                // Check if already in favourites
                val existingDoc = favouritesCollection.document(documentId).get().await()
                if (existingDoc.exists()) {
                    return true
                }
                
                // Create favourite object
                val favourite = Favourite(
                    userId = userId,
                    restaurantId = restaurantId
                )
                
                // Add to Firestore
                favouritesCollection.document(documentId).set(favourite).await()
                
                return true
            } catch (e: Exception) {
                return false
            }
        }

        suspend fun removeFromFavourites(restaurantId: String): Boolean {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return false
            
            try {
                val userId = currentUser.uid
                val documentId = "${userId}_${restaurantId}"
                
                favouritesCollection.document(documentId).delete().await()
                
                return true
            } catch (e: Exception) {
                return false
            }
        }

        suspend fun isInFavourites(restaurantId: String): Boolean {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return false
            
            try {
                val userId = currentUser.uid
                val documentId = "${userId}_${restaurantId}"
                
                val doc = favouritesCollection.document(documentId).get().await()
                return doc.exists()
            } catch (e: Exception) {
                return false
            }
        }
    }
} 