package com.example.SwipeByte.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    private val _isLoggedIn = MutableLiveData<Boolean>(false) // Default to not logged in
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    // Simulate login function
    fun login(username: String, password: String) {
        // Here you'd check credentials, for simplicity, we'll use static values
        if (username == "user" && password == "password") {
            _isLoggedIn.value = true
        } else {
            _isLoggedIn.value = false
        }
    }

    // Simulate logout function
    fun logout() {
        _isLoggedIn.value = false
    }
}
