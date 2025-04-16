package com.swipebyte.ui.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipebyte.ui.db.repository.FirebaseUserRepository
import com.swipebyte.ui.viewmodel.PreferencesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@Composable
fun LocationView(navController: NavController) {

    // State for location
    var latitude by remember { mutableStateOf(43.6532) } // Default to Toronto
    var longitude by remember { mutableStateOf(-79.3832) }

    // State for filter radius (in km)
    var radius by remember { mutableStateOf(5.0) }

    // State for loading
    var isLoading by remember { mutableStateOf(true) }

    // Get context for location services
    val context = LocalContext.current
    val preferencesViewModel = viewModel<PreferencesViewModel>()


    // Check if we have location permission
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // State for map properties
    var mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL,
                isBuildingEnabled = true // Enable 3D buildings
            )
        )
    }

    // State for map UI settings
    var uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false, // We'll provide our own button
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = true
            )
        )
    }

    // Used to control the map camera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 14f)
    }

    // Circle overlay state to show radius
    var circleCenter by remember { mutableStateOf(LatLng(latitude, longitude)) }
    var circleRadius by remember { mutableStateOf(radius * 1000) } // Convert km to meters

    // Scrolling state
    val scrollState = rememberScrollState()

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // Get initial user location
    LaunchedEffect(Unit) {

        try {
            val userLocation = withContext(Dispatchers.IO) {
                FirebaseUserRepository.getInstance().getUserLocation()
            }

            if (userLocation != null) {
                latitude = userLocation.latitude
                longitude = userLocation.longitude
                circleCenter = LatLng(latitude, longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(circleCenter, 14f)
            } else if (hasLocationPermission) {
                // Try getting device location as fallback
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try {
                    val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastKnownLocation != null) {
                        latitude = lastKnownLocation.latitude
                        longitude = lastKnownLocation.longitude
                        circleCenter = LatLng(latitude, longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(circleCenter, 14f)
                    } else {
                        Log.d("LocationView", "No device location available, using default")
                    }
                } catch (e: Exception) {
                    Log.e("LocationView", "Error getting device location: ${e.message}")
                }
            }

            // Get saved radius from preferences
            val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
            radius = sharedPrefs.getFloat("location_radius", 5.0f).toDouble()
            circleRadius = radius * 1000 // Convert km to meters

            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    // Update circle when radius changes
    LaunchedEffect(radius) {
        circleRadius = radius * 1000 // Convert km to meters
    }

    // Main column with scrolling
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Set Your Location",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Maps implementation
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Create a marker state to track position changes
                    val markerState = rememberMarkerState(position = circleCenter)

                    // Update our location when marker position changes
                    LaunchedEffect(markerState.position) {
                        latitude = markerState.position.latitude
                        longitude = markerState.position.longitude
                        circleCenter = markerState.position
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        onMapClick = { latLng ->
                            // Update location when map is clicked
                            latitude = latLng.latitude
                            longitude = latLng.longitude
                            circleCenter = latLng
                            markerState.position = latLng
                        },
                        onMapLoaded = {
                            Log.d("LocationView", "Map loaded successfully")
                        }
                    ) {
                        // Marker at selected location
                        Marker(
                            state = markerState,
                            title = "Selected Location",
                            snippet = "Drag to change location",
                            draggable = true,
                            visible = true,
                            onClick = {
                                // Return false to allow the info window to show
                                false
                            }
                        )

                        // Circle to show radius
                        Circle(
                            center = circleCenter,
                            radius = circleRadius,
                            fillColor = Color(0x33E53935),
                            strokeColor = Color(0xFFE53935),
                            strokeWidth = 2f
                        )
                    }

                    // Location info overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // "My Location" button
                    FloatingActionButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                return@FloatingActionButton
                            }

                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            try {
                                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                if (lastKnownLocation != null) {
                                    latitude = lastKnownLocation.latitude
                                    longitude = lastKnownLocation.longitude

                                    // Update marker and camera
                                    circleCenter = LatLng(latitude, longitude)
                                    markerState.position = circleCenter
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                circleCenter, 14f
                                            ), 1000
                                        )
                                    }
                                } else {
                                    Log.d("LocationView", "No location available from provider")
                                }
                            } catch (e: Exception) {
                                Log.e("LocationView", "Error getting location: ${e.message}")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Radius selection
        Text(
            text = "Restaurant Search Radius: ${String.format("%.1f", radius)} km",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = radius.toFloat(),
            onValueChange = {
                radius = it.toDouble()
            },
            valueRange = 1f..25f,
            steps = 24,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 km",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "25 km",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Helper text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Drag the marker on the map to set your location. Adjust the radius slider to set how far you want to search for restaurants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    // Save location to user profile
                    scope.launch {
                        try {
                            // Update user location
                            FirebaseUserRepository.getInstance().updateUserLocation(latitude, longitude)

                            // Update shared preferences for radius
                            val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                            with(sharedPrefs.edit()) {
                                putFloat("location_radius", radius.toFloat())
                                apply()
                            }
                            preferencesViewModel.loadLocationRadius(context)


                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("LocationView", "Error saving location: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Save Location")
            }
        }

        // Add extra space at the bottom for better scrolling experience
        Spacer(modifier = Modifier.height(24.dp))
    }
}