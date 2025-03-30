package com.example.swipebyte.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.swipebyte.ui.db.observer.PreferencesDataObserver
import com.example.swipebyte.ui.db.observer.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PreferencesViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val preferencesObservable = PreferencesDataObserver.getObservable()

    // Cache for preferences
    private var cachedCuisines: List<String> = emptyList()
    private var cachedPriceRange: List<String> = emptyList()
    private var cachedLocationRadius: Float = 5.0f

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

                // Update the observable with the new preferences
                updateObservable()

                callback()
            }
            .addOnFailureListener {
                // Handle failures - clear cache on failure
                cachedCuisines = emptyList()
                cachedPriceRange = emptyList()
                callback()
            }
    }

    // Load location radius from SharedPreferences
    fun loadLocationRadius(context: Context) {
        val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
        cachedLocationRadius = sharedPrefs.getFloat("location_radius", 5.0f)

        // Update observable with new radius
        updateObservable()
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

                // Update the observable with the new preferences.
                updateObservable()

                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // Update the observable with current preference values.
    private fun updateObservable() {
        val currentPreferences = UserPreferences(
            cuisinePreferences = cachedCuisines,
            pricePreferences = cachedPriceRange,
            locationRadius = cachedLocationRadius
        )
        preferencesObservable.updatePreferences(currentPreferences)

    }
}