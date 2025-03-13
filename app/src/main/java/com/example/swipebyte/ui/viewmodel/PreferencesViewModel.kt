package com.example.swipebyte.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PreferencesViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Cache for preferences
    private var cachedCuisines: List<String> = emptyList()
    private var cachedPriceRange: List<String> = emptyList()

    // Get selected cuisines from cache
    fun getSelectedCuisines(): List<String> {
        return cachedCuisines
    }

    // Get selected price range from cache
    fun getSelectedPriceRange(): List<String> {
        return cachedPriceRange
    }

    // Initialize by loading preferences from Firestore
    fun loadPreferences(callback: () -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback()
            return
        }

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    cachedCuisines = document.get("cuisinePreferences") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    cachedPriceRange = document.get("pricePreferences") as? List<String> ?: emptyList()
                } else {
                    // Clear cache if document doesn't exist
                    cachedCuisines = emptyList()
                    cachedPriceRange = emptyList()
                }
                callback()
            }
            .addOnFailureListener {
                // Handle failures - clear cache on failure
                cachedCuisines = emptyList()
                cachedPriceRange = emptyList()
                callback()
            }
    }

    fun savePreferences(
        selectedCuisines: List<String>,
        priceRange: List<String>,
        callback: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false)
            return
        }

        val preferencesData = hashMapOf(
            "cuisinePreferences" to selectedCuisines,
            "pricePreferences" to priceRange
        )

        firestore.collection("users")
            .document(userId)
            .set(preferencesData, SetOptions.merge())  // Merging to avoid overwriting other fields
            .addOnSuccessListener {
                // Update cache
                cachedCuisines = selectedCuisines
                cachedPriceRange = priceRange
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}