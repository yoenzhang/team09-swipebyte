package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Track if the user is logged in with LiveData
    private val _isLoggedIn = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    init {
        // Check if the user is already logged in
        _isLoggedIn.value = firebaseAuth.currentUser != null
    }

    // Login function with Firebase
    fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    Log.d("AuthViewModel", "Login successful: ${firebaseAuth.currentUser?.email}")
                } else {
                    _isLoggedIn.value = false
                    Log.e("AuthViewModel", "Login failed: ${task.exception?.message}")
                }
            }
    }

    // Optionally handle logout
    fun logout() {
        firebaseAuth.signOut()
        _isLoggedIn.value = false
    }
}
