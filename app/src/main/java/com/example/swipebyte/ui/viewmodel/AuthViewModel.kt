package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.swipebyte.ui.data.models.UserQueryable
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Track if the user is logged in with LiveData
    private val _isLoggedIn = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    private val _currentUserId = MutableLiveData<String?>(null)
    val currentUserId: LiveData<String?> get() = _currentUserId


    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            Log.d("AuthViewModel", "Auth state changed: User = ${user?.email ?: "No user"}")

            if (user != null) {
                _currentUserId.value = user.uid
                user.reload().addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        _isLoggedIn.value = firebaseAuth.currentUser != null
                    } else {
                        Log.e("AuthViewModel", "User reload failed: ${reloadTask.exception?.message}")
                        _isLoggedIn.value = false
                    }
                }
            } else {
                _currentUserId.value = null
                _isLoggedIn.value = false
            }
        }
    }

    // Login function with Firebase
    fun login(email: String?, password: String?, onResult: (Boolean) -> Unit) {
        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.e("AuthViewModel", "Email or password is empty")
            onResult(false)  // Return false to indicate failure
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    _currentUserId.value = firebaseAuth.currentUser?.uid  // Set user ID on login
                    Log.d("AuthViewModel", "Login successful: ${firebaseAuth.currentUser?.email}")
                    UserQueryable.saveUserDataToFirestore()
                    onResult(true)
                } else {
                    _isLoggedIn.value = false
                    Log.e("AuthViewModel", "Login failed: ${task.exception?.message}")
                    onResult(false)
                }
            }
    }

    fun signUp(email: String?, password: String?, onResult: (Boolean) -> Unit) {
        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.e("AuthViewModel", "Email or password is empty")
            onResult(false)  // Return false to indicate failure
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    _currentUserId.value = firebaseAuth.currentUser?.uid  // Set user ID on signup
                    Log.d("AuthViewModel", "Sign Up successful: ${firebaseAuth.currentUser?.email}")
                    UserQueryable.saveUserDataToFirestore()
                    onResult(true)
                } else {
                    _isLoggedIn.value = false
                    Log.e("AuthViewModel", "Sign Up failed: ${task.exception?.message}")
                    onResult(false)
                }
            }
    }
    // Optionally handle logout
    fun logout() {
        firebaseAuth.signOut()
        _isLoggedIn.value = false
        _currentUserId.value = null  // Clear user ID on logout
    }
}
