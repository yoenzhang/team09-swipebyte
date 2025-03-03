package com.example.swipebyte.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.swipebyte.ui.db.models.UserQueryable
import com.example.swipebyte.ui.navigation.AppNavigation
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val LOCATION_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            //RestaurantQueryable.insertData()
            // commenting out as we should have a separate insert when needed
        }
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkAndRequestLocationPermission()

        setContent {
            SwipeByteTheme {
                AppNavigation(authViewModel)
            }
        }
    }

    // Request location permission
    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Permission already granted, start location tracking
            startLocationUpdates()
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                println("✅ Location permission granted")
                startLocationUpdates()
            } else {
                println("❌ Location permission denied")
            }
        }

    // Function to get location updates
    @SuppressLint("MissingPermission") // Ensure permission is granted before calling
    private fun startLocationUpdates() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    println("📍 Current Location: ${location.latitude}, ${location.longitude}")

                    // Call function to update user location in Firestore
                    UserQueryable.updateUserLocation(location.latitude, location.longitude)
                } else {
                    println("⚠️ Location is null")
                }
            }
            .addOnFailureListener { e ->
                println("❌ Error getting location: ${e.message}")
            }
    }
}