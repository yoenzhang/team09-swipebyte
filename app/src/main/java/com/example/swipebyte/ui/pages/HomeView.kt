package com.example.swipebyte.ui.pages

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.data.models.RestaurantQueryable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@Composable
fun HomeView(navController: NavController) {
    // Initialize RestaurantRepository
    val restaurantRepo = RestaurantRepository()

    // State for the list of restaurants
    val restaurantList = remember { mutableStateListOf<Restaurant>() }

    // Previous restaurants stack for undo functionality (limited to 2)
    val previousRestaurants = remember { mutableStateListOf<Restaurant>() }

    // Loading state for when data is being fetched
    var isLoading by remember { mutableStateOf(true) }

    // Current restaurant index
    var currentIndex by remember { mutableStateOf(0) }

    // State for detailed view
    var showDetailScreen by remember { mutableStateOf(false) }

    // State to track if card is being swiped
    var isCardSwiping by remember { mutableStateOf(false) }

    // Coroutine scope for animations
    val coroutineScope = rememberCoroutineScope()

    // Fetch data once when the composable is first launched
    LaunchedEffect(Unit) {
        try {
            // Call fetchRestaurants() to get data from RestaurantRepository
            val fetchedRestaurants = RestaurantQueryable.filterNearbyRestaurants(
                restaurantRepo.getRestaurants())
            // Update the list of restaurants
            restaurantList.clear()
            restaurantList.addAll(fetchedRestaurants.toMutableList())
        } catch (e: Exception) {
            // Handle errors if fetching data fails
            Log.e("HomeView", "Error fetching restaurants: ${e.message}")
        } finally {
            // Set loading state to false after fetching is complete
            isLoading = false
        }
    }

    // Show loading indicator while data is being fetched
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (restaurantList.isEmpty()) {
        // Show empty state when no restaurants are available
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No restaurants found nearby")
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content - Current restaurant card
            if (currentIndex < restaurantList.size) {
                EnhancedRestaurantCard(
                    restaurant = restaurantList[currentIndex],
                    onSwiped = { direction ->
                        // Mark card as swiping
                        isCardSwiping = true

                        // Add current restaurant to previous (maintain max 2)
                        if (previousRestaurants.size >= 2) {
                            previousRestaurants.removeAt(0) // Remove oldest
                        }
                        previousRestaurants.add(restaurantList[currentIndex])

                        // Move to next restaurant with a slight delay to allow animation to complete
                        coroutineScope.launch {
                            delay(200) // Short delay for animation
                            if (currentIndex < restaurantList.size - 1) {
                                currentIndex++
                            }
                            // Reset swiping state
                            isCardSwiping = false
                        }
                    },
                    onCardClick = {
                        showDetailScreen = true
                    }
                )
            } else {
                // No more restaurants to show
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You've seen all restaurants!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Undo button (only visible when there are previous restaurants and not during swipe)
            if (previousRestaurants.isNotEmpty() && !isCardSwiping) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .zIndex(2f)
                ) {
                    Button(
                        onClick = {
                            if (previousRestaurants.isNotEmpty() && !isCardSwiping) {
                                val lastRestaurant = previousRestaurants.last()
                                previousRestaurants.removeLast()

                                // If we're at the end of the list, we need special handling
                                if (currentIndex >= restaurantList.size) {
                                    restaurantList.add(lastRestaurant)
                                    currentIndex = restaurantList.size - 1
                                } else {
                                    // Otherwise, just go back to the previous restaurant
                                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Undo",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo")
                    }
                }
            }

            // Detailed view overlay
            AnimatedVisibility(
                visible = showDetailScreen && currentIndex < restaurantList.size && !isCardSwiping,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (showDetailScreen) 1f else 0f)
            ) {
                if (currentIndex < restaurantList.size) {
                    RestaurantDetailScreen(
                        restaurant = restaurantList[currentIndex],
                        onDismiss = { showDetailScreen = false },
                        onLike = {
                            coroutineScope.launch {
                                showDetailScreen = false
                                // Mark card as swiping
                                isCardSwiping = true

                                delay(300) // Wait for animation to complete

                                // Add to previous for undo functionality (maintain max 2)
                                if (previousRestaurants.size >= 2) {
                                    previousRestaurants.removeAt(0) // Remove oldest
                                }
                                previousRestaurants.add(restaurantList[currentIndex])

                                // Move to next restaurant
                                if (currentIndex < restaurantList.size - 1) {
                                    currentIndex++
                                }

                                // Reset swiping state
                                delay(100)
                                isCardSwiping = false
                            }
                        },
                        onDislike = {
                            coroutineScope.launch {
                                showDetailScreen = false
                                // Mark card as swiping
                                isCardSwiping = true

                                delay(300) // Wait for animation to complete

                                // Add to previous for undo functionality (maintain max 2)
                                if (previousRestaurants.size >= 2) {
                                    previousRestaurants.removeAt(0) // Remove oldest
                                }
                                previousRestaurants.add(restaurantList[currentIndex])

                                // Move to next restaurant
                                if (currentIndex < restaurantList.size - 1) {
                                    currentIndex++
                                }

                                // Reset swiping state
                                delay(100)
                                isCardSwiping = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedRestaurantCard(
    restaurant: Restaurant,
    onSwiped: (String) -> Unit,
    onCardClick: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = 300f

    // Random message state
    var currentSwipeMessage by remember { mutableStateOf("") }

    // Show message overlay state
    var showMessageOverlay by remember { mutableStateOf(false) }

    // Direction state
    var swipeDirection by remember { mutableStateOf("") }

    // List of random positive messages for right swipe
    val positiveMessages = listOf(
        "Okay buddy!",
        "Great choice!",
        "Tasty pick!",
        "You've got good taste!",
        "Foodie approved!",
        "Chef's kiss!",
        "Bon appétit!",
        "Your stomach thanks you!",
        "Dinner date material!",
        "Food coma incoming!"
    )

    // List of random negative messages for left swipe
    val negativeMessages = listOf(
        "Loser...",
        "Alright, don't go",
        "Your loss!",
        "Picky much?",
        "More for me!",
        "Gordon Ramsay would be disappointed",
        "Swipe left on life choices",
        "Food snob alert!",
        "This is why you're hungry",
        "The restaurant dodged a bullet"
    )

    // Reset animation state when the restaurant changes
    LaunchedEffect(restaurant.id) {
        offsetX.snapTo(0f)
        currentSwipeMessage = ""
        showMessageOverlay = false
    }

    // Animation values
    val rotation by animateFloatAsState(
        targetValue = (offsetX.value / 50).coerceIn(-10f, 10f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scale by animateFloatAsState(
        targetValue = 0.9f + (1 - abs(offsetX.value) / 1500f).coerceIn(0f, 0.1f),
        animationSpec = tween(durationMillis = 100)
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            offsetX.value > 100 -> Color(0x1500FF00) // Light green for right swipe
            offsetX.value < -100 -> Color(0x15FF0000) // Light red for left swipe
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 100)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Show swipe message overlay
        if (showMessageOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (swipeDirection == "Right")
                            Color(0xFF4CAF50).copy(alpha = 0.5f) // Semi-transparent green
                        else
                            Color(0xFFF44336).copy(alpha = 0.5f) // Semi-transparent red
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentSwipeMessage,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(restaurant.id) { // Reset detector when restaurant changes
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (abs(offsetX.value) > swipeThreshold) {
                                    val direction = if (offsetX.value > 0) "Right" else "Left"
                                    swipeDirection = direction

                                    // Set a random message based on swipe direction
                                    currentSwipeMessage = if (direction == "Right") {
                                        positiveMessages.random()
                                    } else {
                                        negativeMessages.random()
                                    }

                                    // Show the message overlay
                                    showMessageOverlay = true

                                    val targetValue = if (offsetX.value > 0) 1500f else -1500f

                                    // First animate to show message
                                    offsetX.animateTo(
                                        targetValue = if (offsetX.value > 0) 500f else -500f,
                                        animationSpec = tween(durationMillis = 100)
                                    )

                                    // Pause to show message
                                    delay(10)

                                    // Then complete the animation
                                    offsetX.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(durationMillis = 200)
                                    )

                                    onSwiped(direction)
                                } else {
                                    // Snap back to center
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    currentSwipeMessage = ""
                                    showMessageOverlay = false
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)

                            // Check if we crossed the threshold during drag
                            if (abs(offsetX.value) > swipeThreshold && currentSwipeMessage.isEmpty()) {
                                currentSwipeMessage = if (offsetX.value > 0) {
                                    positiveMessages.random()
                                } else {
                                    negativeMessages.random()
                                }
                            }
                        }
                    }
                }
                .clickable(enabled = !showMessageOverlay) { onCardClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Card image - using first image from the list or fallback
                val imageUrl = if (restaurant.imageUrls.isNotEmpty()) {
                    restaurant.imageUrls[0]
                } else {
                    "https://images.unsplash.com/photo-1514933651103-005eec06c04b" // Fallback image
                }

                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 0f,
                                endY = 300f
                            )
                        )
                        .height((LocalConfiguration.current.screenHeightDp * 0.2f).dp)
                )

                // Content at the bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = restaurant.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = restaurant.cuisineType.joinToString(", "),
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Text(
                            text = " • ",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Text(
                            text = String.format("%.1f", restaurant.yelpRating) + " ⭐",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = String.format(Locale.US, "%.2f", restaurant.distance) + "km away",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Left swipe indicator with random message
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.TopStart)
                        .alpha((-(offsetX.value) / 100f).coerceIn(0f, 1f))
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "NOPE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Right swipe indicator with random message
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.TopEnd)
                        .alpha((offsetX.value / 100f).coerceIn(0f, 1f))
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "LIKE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Tap to view indicator (only visible when not swiping much)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(if (abs(offsetX.value) < 50f) 0.7f else 0f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to view details",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                // Use first image from the list or fallback
                val imageUrl = if (restaurant.imageUrls.isNotEmpty()) {
                    restaurant.imageUrls[0]
                } else {
                    "https://images.unsplash.com/photo-1514933651103-005eec06c04b" // Fallback image
                }

                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Back button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Gradient overlay
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
                        color = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            // Details content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Basic info row
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
                            text = String.format("%.1f", restaurant.yelpRating) + " ⭐",
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider()

                // Description
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = "A wonderful restaurant offering delicious food in a welcoming atmosphere. Perfect for enjoying a meal with friends and family.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Details section
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Address",
                    value = restaurant.address ?: "123 Main Street",
                    isClickable = true
                )

                DetailRow(
                    icon = Icons.Default.Info,
                    title = "Hours",
                    value = "11:00 AM - 10:00 PM"
                )

                DetailRow(
                    icon = Icons.Default.Phone,
                    title = "Phone",
                    value = restaurant.phone ?: "(123) 456-7890"
                )

                DetailRow(
                    icon = Icons.Default.DateRange,
                    title = "Website",
                    value = restaurant.url ?: "www.restaurant.com"
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Extra space at bottom to ensure content is visible above buttons
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Like/Dislike buttons at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 80f
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Dislike button
            FloatingActionButton(
                onClick = onDislike,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color(0xFFF44336),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dislike",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Like button
            FloatingActionButton(
                onClick = onLike,
                shape = CircleShape,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

        // Add Maps button for address if clickable
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