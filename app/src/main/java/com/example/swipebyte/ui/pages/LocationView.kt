package com.example.swipebyte.ui.pages

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.swipebyte.ui.data.models.UserQueryable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    var mapReady by remember { mutableStateOf(false) }

    // Get context for location services
    val context = LocalContext.current

    // Remember map view
    var mapView: MapView? = null
    var googleMap: GoogleMap? = null

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // Get initial user location
    LaunchedEffect(Unit) {
        val userLocation = withContext(Dispatchers.IO) {
            UserQueryable.getUserLocation()
        }

        if (userLocation != null) {
            latitude = userLocation.latitude
            longitude = userLocation.longitude
        } else {
            // Try getting device location as fallback
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    latitude = lastKnownLocation.latitude
                    longitude = lastKnownLocation.longitude
                }
            } catch (e: Exception) {
                // Use default Toronto coordinates
            }
        }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        // Map container
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
                    // Map view
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                mapView = this
                                onCreate(null)
                                getMapAsync { map ->
                                    googleMap = map

                                    // Initial setup
                                    val initialPosition = LatLng(latitude, longitude)
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 14f))

                                    // Add marker for user location
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(initialPosition)
                                            .title("Your Location")
                                            .draggable(true)
                                    )

                                    // Add circle for radius
                                    map.addCircle(
                                        CircleOptions()
                                            .center(initialPosition)
                                            .radius(radius * 1000) // Convert km to meters
                                            .strokeWidth(2f)
                                            .strokeColor(0x55E53935)
                                            .fillColor(0x22E53935)
                                    )

                                    // Handle marker drag events
                                    map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                                        override fun onMarkerDragStart(p0: com.google.android.gms.maps.model.Marker) {}

                                        override fun onMarkerDrag(p0: com.google.android.gms.maps.model.Marker) {}

                                        override fun onMarkerDragEnd(marker: com.google.android.gms.maps.model.Marker) {
                                            // Update position
                                            latitude = marker.position.latitude
                                            longitude = marker.position.longitude

                                            // Update circle
                                            map.clear()
                                            map.addMarker(
                                                MarkerOptions()
                                                    .position(marker.position)
                                                    .title("Your Location")
                                                    .draggable(true)
                                            )
                                            map.addCircle(
                                                CircleOptions()
                                                    .center(marker.position)
                                                    .radius(radius * 1000) // Convert km to meters
                                                    .strokeWidth(2f)
                                                    .strokeColor(0x55E53935)
                                                    .fillColor(0x22E53935)
                                            )
                                        }
                                    })

                                    // Handle map click events
                                    map.setOnMapClickListener { latLng ->
                                        // Update position
                                        latitude = latLng.latitude
                                        longitude = latLng.longitude

                                        // Update marker and circle
                                        map.clear()
                                        map.addMarker(
                                            MarkerOptions()
                                                .position(latLng)
                                                .title("Your Location")
                                                .draggable(true)
                                        )
                                        map.addCircle(
                                            CircleOptions()
                                                .center(latLng)
                                                .radius(radius * 1000) // Convert km to meters
                                                .strokeWidth(2f)
                                                .strokeColor(0x55E53935)
                                                .fillColor(0x22E53935)
                                        )
                                    }

                                    mapReady = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            // Update map view lifecycle
                            mapView?.onResume()

                            // Update circle if radius changes
                            if (mapReady && googleMap != null) {
                                val map = googleMap!!
                                val position = LatLng(latitude, longitude)

                                map.clear()
                                map.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title("Your Location")
                                        .draggable(true)
                                )
                                map.addCircle(
                                    CircleOptions()
                                        .center(position)
                                        .radius(radius * 1000) // Convert km to meters
                                        .strokeWidth(2f)
                                        .strokeColor(0x55E53935)
                                        .fillColor(0x22E53935)
                                )
                            }
                        }
                    )

                    // My location button
                    FloatingActionButton(
                        onClick = {
                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            try {
                                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                if (lastKnownLocation != null) {
                                    latitude = lastKnownLocation.latitude
                                    longitude = lastKnownLocation.longitude

                                    // Update map
                                    googleMap?.let { map ->
                                        val position = LatLng(latitude, longitude)
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))

                                        map.clear()
                                        map.addMarker(
                                            MarkerOptions()
                                                .position(position)
                                                .title("Your Location")
                                                .draggable(true)
                                        )
                                        map.addCircle(
                                            CircleOptions()
                                                .center(position)
                                                .radius(radius * 1000) // Convert km to meters
                                                .strokeWidth(2f)
                                                .strokeColor(0x55E53935)
                                                .fillColor(0x22E53935)
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle location error
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
                    text = "Tap anywhere on the map to set your location or drag the marker. Adjust the radius slider to set how far you want to search for restaurants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

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
                            UserQueryable.updateUserLocation(latitude, longitude)

                            // Update shared preferences for radius
                            val sharedPrefs = context.getSharedPreferences("swipebyte_prefs", Context.MODE_PRIVATE)
                            with(sharedPrefs.edit()) {
                                putFloat("location_radius", radius.toFloat())
                                apply()
                            }

                            // Navigate back to home
                            navController.popBackStack()
                        } catch (e: Exception) {
                            // Error handling
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Save Location")
            }
        }
    }

    // Cleanup map resources
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDestroy()
        }
    }
}