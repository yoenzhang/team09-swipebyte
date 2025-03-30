package com.example.swipebyte.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.swipebyte.ui.db.repository.FirebaseUserRepository
import com.example.swipebyte.ui.navigation.AppNavigation
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel
import com.example.swipebyte.ui.viewmodel.FriendViewModel
import com.example.swipebyte.ui.viewmodel.PreferencesViewModel
import com.example.swipebyte.ui.viewmodel.RestaurantViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val friendViewModel: FriendViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by viewModels()
    private val restaurantViewModel: RestaurantViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // Tag for logging
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            //RestaurantQueryable.insertData()
            // commenting out as we should have a separate insert when needed
        }
        super.onCreate(savedInstanceState)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create the location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d(TAG, "📍 Updated Location: ${location.latitude}, ${location.longitude}")

                    // Update Firestore with new location
                    lifecycleScope.launch {
                        FirebaseUserRepository.getInstance().updateUserLocation(location.latitude, location.longitude)

                        // Also update distances in RestaurantViewModel
                        restaurantViewModel.updateDistances(this@MainActivity)
                    }
                } else {
                    Log.d(TAG, "⚠️ Location is still null")
                }
            }
        }

        // Check permissions before starting location updates
        checkAndRequestLocationPermission()

        // Observe login state to start location updates
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                Log.d(TAG, "User logged in, starting location updates")
                startLocationUpdates() // Refresh location when user logs in
            } else {
                Log.d(TAG, "User logged out, stopping location updates")
                stopLocationUpdates()
            }
        }

        val userId = authViewModel.currentUserId
        setContent {
            SwipeByteTheme {
                AppNavigation(authViewModel, friendViewModel, preferencesViewModel, userId)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart location updates when app is in foreground
        if (authViewModel.isLoggedIn.value == true) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates when app is in background to save battery
        stopLocationUpdates()
    }

    // Request location permission
    private fun checkAndRequestLocationPermission() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Requesting location permissions")
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permission already granted, start location tracking
            Log.d(TAG, "Location permissions already granted")
            startLocationUpdates()
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                Log.d(TAG, "✅ All location permissions granted")
                startLocationUpdates()
            } else {
                Log.d(TAG, "❌ Some location permissions denied")
                // You might want to show a dialog explaining why location is important
            }
        }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // First check if we have permission
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.d(TAG, "Cannot start location updates: Permission not granted")
            return
        }

        try {
            // Get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")

                    // Update Firestore with this location
                    lifecycleScope.launch {
                        FirebaseUserRepository.getInstance().updateUserLocation(location.latitude, location.longitude)
                    }
                }
            }

            // Create location request for periodic updates
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(30000)
                .setMaxUpdateDelayMillis(60000)
                .build()

            // Remove any existing callbacks to avoid duplicates
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }

            // Request location updates
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    it,
                    Looper.getMainLooper()
                )
                Log.d(TAG, "Location updates started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
                Log.d(TAG, "Location updates stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates: ${e.message}")
        }
    }
}