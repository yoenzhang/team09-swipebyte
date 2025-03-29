package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.swipebyte.ui.data.models.UserQueryable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

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

            if (user != null) {
                _currentUserId.value = user.uid
                user.reload().addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        _isLoggedIn.value = firebaseAuth.currentUser != null
                    } else {
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
                    UserQueryable.saveUserDataToFirestore()
                    onResult(true)
                } else {
                    _isLoggedIn.value = false
                    onResult(false)
                }
            }
    }

    fun signUp(name: String, email: String?, password: String?, onResult: (Boolean) -> Unit) {
        if (name.isEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty()) {
            onResult(false)  // Return false to indicate failure
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    _currentUserId.value = firebaseAuth.currentUser?.uid  // Set user ID on signup

                    // Update the display name in Firebase Auth
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    firebaseAuth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d("AuthViewModel", "User profile updated with name: $name")
                            } else {
                                Log.e("AuthViewModel", "Failed to update display name: ${profileTask.exception?.message}")
                            }

                            // Save to Firestore regardless of profile update success
                            UserQueryable.saveUserDataToFirestore(name)
                            onResult(true)
                        }
                } else {
                    _isLoggedIn.value = false
                    onResult(false)
                }
            }
    }

    // Logout function
    fun logout() {
        firebaseAuth.signOut()
        _isLoggedIn.value = false
        _currentUserId.value = null  // Clear user ID on logout
    }

    // Get current user
    fun getCurrentUser() = firebaseAuth.currentUser

    // Update display name
    fun updateDisplayName(newDisplayName: String, callback: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback(false, "No user is signed in")
            return
        }

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newDisplayName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update the display name in Firestore too
                    UserQueryable.saveUserDataToFirestore(newDisplayName)
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Update password
    fun updatePassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback(false, "No user is signed in")
            return
        }

        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message ?: "Password update failed")
                }
            }
    }
    fun reauthenticateAndUpdatePassword(currentPassword: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback(false, "No user is signed in")
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            callback(false, "User email is missing")
            return
        }

        // Create credential for re-authentication
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        // Re-authenticate user
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Now update the password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                callback(true, null)
                            } else {
                                callback(false, updateTask.exception?.message ?: "Password update failed")
                            }
                        }
                } else {
                    callback(false, "Current password is incorrect or authentication expired")
                }
            }
    }
}
