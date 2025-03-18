package com.example.swipebyte.ui.pages

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.ui.data.models.Restaurant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swipebyte.R
import com.example.swipebyte.ui.data.models.SwipeQueryable
import com.example.swipebyte.ui.data.models.YelpHours
import com.example.swipebyte.ui.navigation.Screen
import com.example.swipebyte.ui.viewmodel.RestaurantViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.filled.Refresh


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedRestaurantCard(
    restaurant: Restaurant,
    onSwiped: (String) -> Unit,
    onDetailsClick: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val horizontalSwipeThreshold = 300f
    val verticalSwipeThreshold = 300f

    // Random message state
    var currentSwipeMessage by remember { mutableStateOf("") }

    // Show message overlay state
    var showMessageOverlay by remember { mutableStateOf(false) }

    // Direction state
    var swipeDirection by remember { mutableStateOf("") }

    // Current image index for gallery
    val pagerState = rememberPagerState { 5 } // 5 images max

    // Important: Move this out of remember block so it updates when restaurant changes
    // Get current restaurant's image URL
    val baseUrl = if (restaurant.imageUrls.isNotEmpty()) restaurant.imageUrls[0] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    val secondImage = if (restaurant.imageUrls.size > 1) restaurant.imageUrls[1] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    val thirdImage = if (restaurant.imageUrls.size > 3) restaurant.imageUrls[2] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    val fourthImage = if (restaurant.imageUrls.size > 4) restaurant.imageUrls[3] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    val fifthImage = if (restaurant.imageUrls.size > 5) restaurant.imageUrls[4] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    // Generate image URLs list with the current restaurant's image as the first one
    val imageUrls = listOf(
        baseUrl,
        secondImage,
        thirdImage,
        fourthImage,
        fifthImage
    )

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
        offsetY.snapTo(0f)
        currentSwipeMessage = ""
        showMessageOverlay = false
        // Also reset pager state when restaurant changes
        pagerState.scrollToPage(0)
    }

    // Animation values
    val rotationX by animateFloatAsState(
        targetValue = (offsetX.value / 50).coerceIn(-10f, 10f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scale by animateFloatAsState(
        targetValue = 0.9f + (1 - (abs(offsetX.value) + abs(offsetY.value)) / 1500f).coerceIn(0f, 0.1f),
        animationSpec = tween(durationMillis = 100)
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            offsetX.value > 100 -> Color(0x1500FF00) // Light green for right swipe
            offsetX.value < -100 -> Color(0x15FF0000) // Light red for left swipe
            offsetY.value < -100 -> Color(0x150000FF) // Light blue for up swipe
            offsetY.value > 100 -> Color(0x15FF00FF) // Light purple for down swipe
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
                        color = when (swipeDirection) {
                            "Right" -> Color(0xFF4CAF50).copy(alpha = 0.5f) // Semi-transparent green
                            "Left" -> Color(0xFFF44336).copy(alpha = 0.5f) // Semi-transparent red
                            "Up" -> Color(0xFF2196F3).copy(alpha = 0.5f) // Semi-transparent blue
                            "Down" -> Color(0xFF9C27B0).copy(alpha = 0.5f) // Semi-transparent purple
                            else -> Color.Transparent
                        }
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

        // Main card with swipe gestures
        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        offsetX.value.roundToInt(),
                        offsetY.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    rotationZ = rotationX
                    scaleX = scale
                    scaleY = scale
                }
                // Combined horizontal and vertical gesture detection
                .pointerInput(restaurant.id) {
                    var initialOffsetX = 0f
                    var initialOffsetY = 0f

                    detectDragGestures(
                        onDragStart = {
                            initialOffsetX = offsetX.value
                            initialOffsetY = offsetY.value
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // Check if the horizontal swipe threshold is met
                                if (abs(offsetX.value) > horizontalSwipeThreshold && abs(offsetX.value) > abs(offsetY.value)) {
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
                                    delay(5)

                                    // Then complete the animation
                                    offsetX.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(durationMillis = 200)
                                    )

                                    onSwiped(direction)
                                    // record swipe
                                    SwipeQueryable.recordSwipe(restaurant.id, restaurant.name, direction == "Right")
                                }
                                // Check if the vertical swipe threshold is met
                                else if (abs(offsetY.value) > verticalSwipeThreshold && abs(offsetY.value) > abs(offsetX.value)) {
                                    val direction = if (offsetY.value < 0) "Up" else "Down"
                                    swipeDirection = direction

                                    // Set a simple message for vertical swipes
                                    currentSwipeMessage = if (direction == "Up") {
                                        "Next restaurant!"
                                    } else {
                                        "Previous restaurant!"
                                    }

                                    // Show the message overlay
                                    showMessageOverlay = true

                                    val targetValue = if (offsetY.value < 0) -1500f else 1500f

                                    // First animate to show message
                                    offsetY.animateTo(
                                        targetValue = if (offsetY.value < 0) -500f else 500f,
                                        animationSpec = tween(durationMillis = 100)
                                    )

                                    // Pause to show message
                                    delay(5)

                                    // Then complete the animation
                                    offsetY.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(durationMillis = 200)
                                    )

                                    // Call the appropriate vertical swipe function
                                    if (direction == "Up") {
                                        onSwipeUp()
                                    } else {
                                        onSwipeDown()
                                    }
                                } else {
                                    // Snap back to center if no threshold is met
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    offsetY.animateTo(
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
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Determine whether this is more of a horizontal or vertical drag
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    // Horizontal drag - update X offset
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    // Reset Y offset
                                    if (abs(offsetY.value) > 0) {
                                        offsetY.snapTo(offsetY.value * 0.9f)
                                    }
                                } else {
                                    // Vertical drag - update Y offset
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                    // Reset X offset
                                    if (abs(offsetX.value) > 0) {
                                        offsetX.snapTo(offsetX.value * 0.9f)
                                    }
                                }

                                // Check if we crossed the threshold during drag for horizontal swipes
                                if (abs(offsetX.value) > horizontalSwipeThreshold &&
                                    currentSwipeMessage.isEmpty() &&
                                    abs(offsetX.value) > abs(offsetY.value)) {

                                    currentSwipeMessage = if (offsetX.value > 0) {
                                        positiveMessages.random()
                                    } else {
                                        negativeMessages.random()
                                    }
                                }

                                // Check if we crossed the threshold during drag for vertical swipes
                                if (abs(offsetY.value) > verticalSwipeThreshold &&
                                    currentSwipeMessage.isEmpty() &&
                                    abs(offsetY.value) > abs(offsetX.value)) {

                                    currentSwipeMessage = if (offsetY.value < 0) {
                                        "Next restaurant!"
                                    } else {
                                        "Previous restaurant!"
                                    }
                                }
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Image gallery with left/right navigation
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false // Disable pager swiping to prevent conflicts
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Image
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUrls[page]),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Left half click area (Use clickable with indication = null to prevent ripple that interferes with swipe)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(LocalConfiguration.current.screenWidthDp.dp / 2)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        // Go to previous image (with wrap-around)
                                        val targetPage = if (pagerState.currentPage > 0)
                                            pagerState.currentPage - 1
                                        else
                                            pagerState.pageCount - 1
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                }
                        )

                        // Right half click area
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(LocalConfiguration.current.screenWidthDp.dp / 2)
                                .align(Alignment.TopEnd)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        // Go to next image (with wrap-around)
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage + 1) % pagerState.pageCount
                                        )
                                    }
                                }
                        )
                    }
                }

                // Left swipe indicator
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

                // Right swipe indicator
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

                // Up swipe indicator
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.TopCenter)
                        .alpha((-(offsetY.value) / 100f).coerceIn(0f, 1f))
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "NEXT",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Down swipe indicator
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.BottomCenter)
                        .alpha((offsetY.value / 100f).coerceIn(0f, 1f))
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "PREV",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom info panel with details button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    // Main info rectangle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Restaurant info (with padding on the right side for details button)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .padding(end = 56.dp) // Space for details button only
                            ) {
                                Text(
                                    text = restaurant.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = String.format(Locale.US, "%.1f", restaurant.yelpRating),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = " • ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )

                                    Text(
                                        text = restaurant.cuisineType.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "%.2f", restaurant.distance) + "km away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            // Details button on the right side
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                            ) {
                                IconButton(
                                    onClick = onDetailsClick,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935))
                                        .size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "View Details",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Image pagination indicator
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until pagerState.pageCount) {
                                val isSelected = i == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(width = if (isSelected) 16.dp else 8.dp, height = 8.dp)
                                        .background(
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeView(navController: NavController) {
    // Get context for accessing SharedPreferences
    val context = LocalContext.current

    // Create an instance of RestaurantViewModel
    val restaurantViewModel = viewModel<RestaurantViewModel>()

    // Observe restaurants from ViewModel
    val restaurants by restaurantViewModel.restaurants.observeAsState(emptyList())
    val isLoading by restaurantViewModel.isLoading.observeAsState(true)
    val error by restaurantViewModel.error.observeAsState(null)

    // State for the list of restaurants (using local copy for swipe functionality)
    val restaurantList = remember { mutableStateListOf<Restaurant>() }

    // Current restaurant index
    var currentIndex by remember { mutableStateOf(0) }

    // State for detailed view
    var showDetailScreen by remember { mutableStateOf(false) }

    // State to track if card is being swiped
    var isCardSwiping by remember { mutableStateOf(false) }

    // State to track when to reload data (increased when we need a refresh)
    var refreshTrigger by remember { mutableStateOf(0) }

    // Coroutine scope for animations
    val coroutineScope = rememberCoroutineScope()

    // Function to navigate to the next restaurant
    val goToNextRestaurant = {
        coroutineScope.launch {
            if (currentIndex < restaurantList.size - 1) {
                isCardSwiping = true
                delay(5) // Allow animation to complete
                currentIndex++
                delay(5)
                isCardSwiping = false
            }
        }
    }

    // Function to navigate to the previous restaurant
    val goToPreviousRestaurant = {
        coroutineScope.launch {
            if (currentIndex > 0) {
                isCardSwiping = true
                delay(5) // Allow animation to complete
                currentIndex--
                delay(5)
                isCardSwiping = false
            }
        }
    }

    // Load restaurants data when the composable is first launched or refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        Log.d("HomeView", "Loading restaurants, refreshTrigger: $refreshTrigger")
        // Don't set isLoading here as it's controlled by the ViewModel
        restaurantViewModel.loadRestaurants(context)
    }

    // Update local restaurant list when ViewModel data changes
    LaunchedEffect(restaurants) {
        restaurantList.clear()
        restaurantList.addAll(restaurants)
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            // Wait 15 minutes before refreshing
            delay(15*60 * 1000)
            Log.d("HomeView", "Auto-refreshing restaurant list to check for newly available restaurants")
            restaurantViewModel.loadRestaurants(context)
        }
    }

    // And replace them with this improved version:
    DisposableEffect(navController) {
        // Keep track of the previous destination to determine if we're truly returning to HomeView
        var previousDestination = ""

        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val currentRoute = destination.route ?: ""

            // Only trigger refresh when returning to Home from another screen (not initial load)
            if (currentRoute == Screen.Home.route && previousDestination != "" && previousDestination != Screen.Home.route) {
                Log.d("HomeView", "Returning to HomeView from $previousDestination, forcing refresh")

                // Force a refresh by incrementing the trigger and calling refresh with forceRefresh=true
                refreshTrigger++
                coroutineScope.launch {
                    // Small delay to ensure the view is ready
                    delay(100)
                    restaurantViewModel.refreshRestaurants(context)
                }
            }

            // Record current destination as previous for next navigation event
            previousDestination = currentRoute
        }

        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // Improved top app bar with SwipeByte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE53935),  // Darker red
                            Color(0xFFEF5350)   // Lighter red
                        )
                    )
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Image instead of Icon
                Image(
                    painter = painterResource(id = R.drawable.bird_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // App name with style
                Text(
                    text = "SwipeByte",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Refresh button
                IconButton(
                    onClick = {
                        refreshTrigger++  // Trigger refresh
                        restaurantViewModel.refreshRestaurants(context)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Restaurants",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Main content
        Box(modifier = Modifier.weight(1f)) {
            // Show loading indicator while data is being fetched
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (restaurantList.isEmpty()) {
                // Show empty state when no restaurants are available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No new restaurants available",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Check back later for new restaurants. Restaurants you've swiped on will become available again after 24 hours.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { restaurantViewModel.refreshRestaurants(context) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content - Current restaurant card
                    if (currentIndex < restaurantList.size) {
                        EnhancedRestaurantCard(
                            restaurant = restaurantList[currentIndex],
                            // In the EnhancedRestaurantCard instantiation in HomeView, replace the onSwiped function with this improved version:

                            onSwiped = { direction ->
                                // Mark card as swiping
                                isCardSwiping = true

                                // Record the swipe in Firestore first (ensure it's complete)
                                coroutineScope.launch {
                                    val isLiked = direction == "Right"
                                    // Add await() here to ensure the swipe is recorded before moving on
                                    try {
                                        Log.d("HomeView", "Recording swipe ${if (isLiked) "LIKE" else "DISLIKE"} for ${restaurantList[currentIndex].name}")

                                        // Record the swipe and wait for completion
                                        SwipeQueryable.recordSwipe(restaurantList[currentIndex].id, restaurantList[currentIndex].name, isLiked)

                                        // Remove the swiped restaurant from the current list immediately
                                        val currentRestaurant = restaurantList[currentIndex]

                                        // Wait for Firebase to register the change
                                        delay(300)

                                        // Now refresh the list to ensure it's updated everywhere
                                        restaurantViewModel.refreshRestaurants(context)

                                        // Also remove the restaurant from our local list to ensure immediate visual feedback
                                        restaurantList.remove(currentRestaurant)

                                        // If this was the last restaurant, there's nothing left to show
                                        // Otherwise, we don't need to increment the index since removing the current item
                                        // effectively makes the next item appear at the current index

                                        // Reset swiping state
                                        isCardSwiping = false

                                        // Force UI update (even though state should have changed already)
                                        refreshTrigger++
                                    } catch (e: Exception) {
                                        Log.e("HomeView", "Error recording swipe: ${e.message}", e)
                                        isCardSwiping = false
                                    }
                                }
                            },
                            onDetailsClick = {
                                showDetailScreen = true
                            },
                            onSwipeUp = {
                                // Go to next restaurant on swipe up
                                goToNextRestaurant()
                            },
                            onSwipeDown = {
                                // Go to previous restaurant on swipe down
                                goToPreviousRestaurant()
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

                    // Detailed view overlay
                    androidx.compose.animation.AnimatedVisibility(
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

                                        // record swipe from button
                                        val curRestaurant = restaurantList[currentIndex]
                                        SwipeQueryable.recordSwipe(curRestaurant.id, curRestaurant.name, true)

                                        delay(10) // Wait for animation to complete

                                        // Move to next restaurant
                                        if (currentIndex < restaurantList.size - 1) {
                                            currentIndex++
                                        }

                                        // Reset swiping state
                                        delay(10)
                                        isCardSwiping = false
                                    }
                                },
                                onDislike = {
                                    coroutineScope.launch {
                                        showDetailScreen = false
                                        // Mark card as swiping
                                        isCardSwiping = true

                                        // record swipe from button
                                        val curRestaurant = restaurantList[currentIndex]
                                        SwipeQueryable.recordSwipe(curRestaurant.id, curRestaurant.name, false)

                                        delay(10) // Wait for animation to complete

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

    // Intercept and consume all gesture inputs to prevent swiping on this screen
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Add this modifier to consume all pointer input events
            .pointerInput(Unit) {
                // Detect and consume all touch events to prevent them from propagating
                // to parent composables that might handle swipes
                detectDragGestures { _, _ -> }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header image with explicit gesture blocking
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    // Explicitly consume all touch events on the image
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { /* Do nothing, just consume the click */ }
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            // Details content - the scroll behavior is preserved but swipes won't trigger card gestures
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                HorizontalDivider()

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

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Details section
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Address",
                    value = restaurant.address ,
                    isClickable = true
                )

                RestaurantHoursSection(restaurant.hours)

                DetailRow(
                    icon = Icons.Default.Phone,
                    title = "Phone",
                    value = restaurant.phone
                )

                DetailRow(
                    icon = Icons.Default.DateRange,
                    title = "Website",
                    value = restaurant.url
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

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


@Composable
fun RestaurantHoursSection(
    hours: List<YelpHours>?
) {
    // Return early if no hours data is available
    if (hours.isNullOrEmpty() || hours.first().open.isNullOrEmpty()) {
        DetailRow(
            icon = Icons.Default.Info,
            title = "Hours",
            value = "Hours information not available"
        )
        return
    }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val regularHours = hours.find { it.hours_type == "REGULAR" }
    val isOpenNow = regularHours?.is_open_now ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Hours header with open/closed indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
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

            // Open/Closed status chip
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

        // Hours by day
        regularHours?.open?.sortedBy { it.day }?.forEach { openHours ->
            val day = openHours.day
            if (day != null && day >= 0 && day < 7) {
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

// Helper function to format time from "1000" to "10:00 AM"
private fun formatTime(time: String?): String {
    if (time.isNullOrEmpty()) return "Closed"

    try {
        val hour = time.take(2).toInt()
        val minute = time.takeLast(2)

        val hourIn12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        val amPm = if (hour >= 12) "PM" else "AM"

        return "$hourIn12:$minute $amPm"
    } catch (e: Exception) {
        return time
    }
}