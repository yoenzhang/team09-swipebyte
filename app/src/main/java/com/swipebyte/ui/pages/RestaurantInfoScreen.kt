package com.swipebyte.ui.pages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swipebyte.ui.data.models.Restaurant
import com.swipebyte.ui.data.models.YelpHours
import java.util.Locale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import java.util.Calendar
import com.swipebyte.ui.db.repository.FavouriteQueryable

@Composable
fun RestaurantInfoScreen(
    restaurant: Restaurant,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Track if this restaurant is in favorites
    var isInFavorites by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Check if restaurant is in favorites when screen loads
    LaunchedEffect(restaurant.id) {
        isLoading = true
        try {
            isInFavorites = FavouriteQueryable.isInFavourites(restaurant.id)
        } finally {
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Consume gestures so that underlying views don't receive swipes
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Image and Restaurant Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                val imageUrl = if (restaurant.imageUrls.isNotEmpty()) restaurant.imageUrls[0]
                else "https://images.unsplash.com/photo-1514933651103-005eec06c04b" // Fallback URL
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Back button to dismiss detail view
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                // Favorite button in top-right corner
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val success = if (!isInFavorites) {
                                    FavouriteQueryable.addToFavourites(restaurant.id)
                                } else {
                                    FavouriteQueryable.removeFromFavourites(restaurant.id)
                                }
                                
                                if (success) {
                                    isInFavorites = !isInFavorites
                                    Toast.makeText(
                                        context,
                                        if (isInFavorites) "Added to favorites" else "Removed from favorites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to update favorites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .align(Alignment.TopEnd)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isInFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isInFavorites) "Remove from favorites" else "Add to favorites",
                            tint = if (isInFavorites) Color(0xFFFF4081) else Color.White
                        )
                    }
                }
                
                // Gradient overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.6f)
                                ),
                                startY = 0f,
                                endY = 250f
                            )
                        )
                )
                // Restaurant name overlay
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
            // Restaurant Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Basic Info Row: Cuisine, Rating, Price Range, Distance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = restaurant.cuisineType.joinToString(", "),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", restaurant.yelpRating) + " ⭐",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = restaurant.priceRange ?: "$$",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", restaurant.distance) + "km away",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                HorizontalDivider()
                // About Section
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                // Replace the placeholder text below with actual restaurant description if available
                Text(
                    text = "Taste buds are in heaven",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // Additional Details Section
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                RestaurantDetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Address",
                    value = restaurant.address,
                    isClickable = true
                )
                CustomRestaurantHoursSection(restaurant.hours)
                RestaurantDetailRow(
                    icon = Icons.Default.Phone,
                    title = "Phone",
                    value = restaurant.phone
                )
                RestaurantDetailRow(
                    icon = Icons.Default.DateRange,
                    title = "Website",
                    value = restaurant.url
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RestaurantDetailRow(
    icon: ImageVector,
    title: String,
    value: String,
    isClickable: Boolean = false,
    onValueClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isClickable && onValueClick != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onValueClick() }
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        // Optional: if title is "Address", add a maps button if no click handler is provided
        if (isClickable && title.equals("Address", ignoreCase = true) && onValueClick == null) {
            IconButton(
                onClick = {
                    val encodedAddress = Uri.encode(value)
                    val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
                    val intent = Intent(Intent.ACTION_VIEW, mapUri)
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Open in Google Maps",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomRestaurantHoursSection(
    hours: List<YelpHours>?
) {
    if (hours.isNullOrEmpty() || hours.first().open.isNullOrEmpty()) {
        RestaurantDetailRow(
            icon = Icons.Default.DateRange,
            title = "Hours",
            value = "Hours information not available"
        )
        return
    }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val regularHours = hours.find { it.hours_type == "REGULAR" }
    // Get the current day and time
    val calendar = Calendar.getInstance()
    // Convert Java day of week (Sunday=1, Monday=2, ..., Saturday=7) to Yelp format (Monday=0, ..., Sunday=6)
    val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

    // Current time in 24-hour format as a string (e.g., "1430" for 2:30 PM)
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTime = String.format(Locale.US, "%02d%02d", currentHour, currentMinute)

    // Calculate if open now
    val isOpenNow = regularHours?.open?.any { openHours ->
        openHours.day == currentDay &&
                currentTime >= (openHours.start ?: "0000") &&
                currentTime <= (openHours.end ?: "2359")
    } ?: false
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Hours",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOpenNow) Color(0xFFACE7B3) else Color(0xFFFFABAB)
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (isOpenNow) "Open now" else "Closed now",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOpenNow) Color(0xFF0C6216) else Color(0xFF9A0007),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        regularHours?.open?.sortedBy { it.day }?.forEach { openHours ->
            val day = openHours.day
            if (day != null && day in 0..6) {
                val dayName = daysOfWeek[day]
                val startTime = formatTime(openHours.start)
                val endTime = formatTime(openHours.end)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$startTime - $endTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(time: String?): String {
    if (time.isNullOrEmpty()) return "Closed"
    return try {
        val hour = time.take(2).toInt()
        val minute = time.takeLast(2)
        val hourIn12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour >= 12) "PM" else "AM"
        "$hourIn12:$minute $amPm"
    } catch (e: Exception) {
        time
    }
}