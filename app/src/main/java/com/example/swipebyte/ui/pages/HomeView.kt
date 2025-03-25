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
import java.util.Calendar
import androidx.compose.material.icons.filled.Refresh
import com.example.swipebyte.ui.viewmodel.PreferencesViewModel


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

    var currentSwipeMessage by remember { mutableStateOf("") }
    var showMessageOverlay by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf("") }

    // Gallery control
    val pagerState = rememberPagerState { 5 }

    // Prepare image URLs
    val baseUrl = if (restaurant.imageUrls.isNotEmpty()) restaurant.imageUrls[0] else
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b"

    val secondImage = if (restaurant.imageUrls.size > 1) restaurant.imageUrls[1] else baseUrl
    val thirdImage = if (restaurant.imageUrls.size > 3) restaurant.imageUrls[2] else baseUrl
    val fourthImage = if (restaurant.imageUrls.size > 4) restaurant.imageUrls[3] else baseUrl
    val fifthImage = if (restaurant.imageUrls.size > 5) restaurant.imageUrls[4] else baseUrl

    val imageUrls = listOf(baseUrl, secondImage, thirdImage, fourthImage, fifthImage)

    // Swipe message lists
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

    // Reset state when restaurant changes
    LaunchedEffect(restaurant.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        currentSwipeMessage = ""
        showMessageOverlay = false
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
            offsetX.value > 100 -> Color(0x1500FF00) // Green for right swipe
            offsetX.value < -100 -> Color(0x15FF0000) // Red for left swipe
            offsetY.value < -100 -> Color(0x150000FF) // Blue for up swipe
            offsetY.value > 100 -> Color(0x15FF00FF) // Purple for down swipe
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
        // Swipe message overlay
        if (showMessageOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = when (swipeDirection) {
                            "Right" -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                            "Left" -> Color(0xFFF44336).copy(alpha = 0.5f)
                            "Up" -> Color(0xFF2196F3).copy(alpha = 0.5f)
                            "Down" -> Color(0xFF9C27B0).copy(alpha = 0.5f)
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

        // Main swipeable card
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
                                // Handle horizontal swipe
                                if (abs(offsetX.value) > horizontalSwipeThreshold && abs(offsetX.value) > abs(offsetY.value)) {
                                    val direction = if (offsetX.value > 0) "Right" else "Left"
                                    swipeDirection = direction
                                    currentSwipeMessage = if (direction == "Right") {
                                        positiveMessages.random()
                                    } else {
                                        negativeMessages.random()
                                    }
                                    showMessageOverlay = true

                                    val targetValue = if (offsetX.value > 0) 1500f else -1500f
                                    offsetX.animateTo(
                                        targetValue = if (offsetX.value > 0) 500f else -500f,
                                        animationSpec = tween(durationMillis = 100)
                                    )
                                    delay(5)
                                    offsetX.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(durationMillis = 200)
                                    )

                                    onSwiped(direction)
                                    SwipeQueryable.recordSwipe(restaurant.id, restaurant.name, direction == "Right")
                                }
                                // Handle vertical swipe
                                else if (abs(offsetY.value) > verticalSwipeThreshold && abs(offsetY.value) > abs(offsetX.value)) {
                                    val direction = if (offsetY.value < 0) "Up" else "Down"
                                    swipeDirection = direction
                                    currentSwipeMessage = if (direction == "Up") {
                                        "Next restaurant!"
                                    } else {
                                        "Previous restaurant!"
                                    }
                                    showMessageOverlay = true

                                    val targetValue = if (offsetY.value < 0) -1500f else 1500f
                                    offsetY.animateTo(
                                        targetValue = if (offsetY.value < 0) -500f else 500f,
                                        animationSpec = tween(durationMillis = 100)
                                    )
                                    delay(5)
                                    offsetY.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(durationMillis = 200)
                                    )

                                    if (direction == "Up") {
                                        onSwipeUp()
                                    } else {
                                        onSwipeDown()
                                    }
                                } else {
                                    // Return to center if no threshold met
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
                                // Prioritize horizontal or vertical based on drag direction
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    if (abs(offsetY.value) > 0) {
                                        offsetY.snapTo(offsetY.value * 0.9f)
                                    }
                                } else {
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                    if (abs(offsetX.value) > 0) {
                                        offsetX.snapTo(offsetX.value * 0.9f)
                                    }
                                }

                                // Show messages when threshold crossed during drag
                                if (abs(offsetX.value) > horizontalSwipeThreshold &&
                                    currentSwipeMessage.isEmpty() &&
                                    abs(offsetX.value) > abs(offsetY.value)) {

                                    currentSwipeMessage = if (offsetX.value > 0) {
                                        positiveMessages.random()
                                    } else {
                                        negativeMessages.random()
                                    }
                                }

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
                // Image gallery
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false // Disable pager swiping to avoid conflicts
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUrls[page]),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Left image navigation area
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(LocalConfiguration.current.screenWidthDp.dp / 2)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        val targetPage = if (pagerState.currentPage > 0)
                                            pagerState.currentPage - 1
                                        else
                                            pagerState.pageCount - 1
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                }
                        )

                        // Right image navigation area
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
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage + 1) % pagerState.pageCount
                                        )
                                    }
                                }
                        )
                    }
                }

                // Swipe indicators
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

                // Restaurant info panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .padding(end = 56.dp)
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

                            // Details button
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

                // Image pagination dots
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
    // Grab the context for SharedPreferences
    val context = LocalContext.current

    // Setup our restaurant view model
    val restaurantViewModel = viewModel<RestaurantViewModel>()
    val preferencesViewModel = viewModel<PreferencesViewModel>()

    // Let's watch for changes in our data
    val restaurants by restaurantViewModel.restaurants.observeAsState(emptyList())
    val isLoading by restaurantViewModel.isLoading.observeAsState(true)
    val error by restaurantViewModel.error.observeAsState(null)

    // Need a local copy for our swipe functionality
    val restaurantList = remember { mutableStateListOf<Restaurant>() }

    // Keep track of which restaurant we're looking at
    var currentIndex by remember { mutableStateOf(0) }

    // For showing the detailed view
    var showDetailScreen by remember { mutableStateOf(false) }

    // Track if a card is being swiped (for animations)
    var isCardSwiping by remember { mutableStateOf(false) }

    // Need this for animations
    val coroutineScope = rememberCoroutineScope()

    // Go to next restaurant when swiping
    val goToNextRestaurant = {
        coroutineScope.launch {
            if (currentIndex < restaurantList.size - 1) {
                isCardSwiping = true
                delay(5)
                currentIndex++
                delay(5)
                isCardSwiping = false
            }
        }
    }

    // Go back to previous restaurant
    val goToPreviousRestaurant = {
        coroutineScope.launch {
            if (currentIndex > 0) {
                isCardSwiping = true
                delay(5)
                currentIndex--
                delay(5)
                isCardSwiping = false
            }
        }
    }

    // Track refresh trigger
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load preferences and restaurants when we start or need to refresh
    LaunchedEffect(refreshTrigger) {
        Log.d("HomeView", "Loading restaurants, refreshTrigger: $refreshTrigger")

        // Load location radius from SharedPreferences into the PreferencesViewModel
        preferencesViewModel.loadLocationRadius(context)

        // Load and apply cuisine/price preferences
        preferencesViewModel.loadPreferences {
            // After preferences are loaded, load the restaurants
            restaurantViewModel.loadRestaurants(context)
        }
    }

    // Sync our local list with the ViewModel data
    LaunchedEffect(restaurants) {
        restaurantList.clear()
        restaurantList.addAll(restaurants)
    }

    // Check for new restaurants every 15 minutes
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(15*60 * 1000)
            Log.d("HomeView", "Looking for new restaurants - auto-refresh")
            restaurantViewModel.loadRestaurants(context)
        }
    }

    // Refresh when coming back to this screen
    DisposableEffect(navController) {
        var previousDestination = ""

        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val currentRoute = destination.route ?: ""

            if (currentRoute == Screen.Home.route && previousDestination != "" && previousDestination != Screen.Home.route) {
                Log.d("HomeView", "Back to home from $previousDestination - refreshing")
                refreshTrigger++
                coroutineScope.launch {
                    delay(100)
                    restaurantViewModel.refreshRestaurants(context)
                }
            }

            previousDestination = currentRoute
        }

        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cool gradient app bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE53935),
                            Color(0xFFEF5350)
                        )
                    )
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bird_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

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

                IconButton(
                    onClick = {
                        refreshTrigger++
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

        // Main content area
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (restaurantList.isEmpty()) {
                // No restaurants? Show a nice empty state
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
                    // Show the current restaurant card
                    if (currentIndex < restaurantList.size) {
                        EnhancedRestaurantCard(
                            restaurant = restaurantList[currentIndex],
                            onSwiped = { direction ->
                                // Mark card as swiping
                                isCardSwiping = true

                                coroutineScope.launch {
                                    val isLiked = direction == "Right"
                                    try {
                                        Log.d("HomeView", "Recording ${if (isLiked) "LIKE" else "DISLIKE"} for ${restaurantList[currentIndex].name}")

                                        // Save the swipe and wait for it to complete
                                        SwipeQueryable.recordSwipe(restaurantList[currentIndex].id, restaurantList[currentIndex].name, isLiked)

                                        val currentRestaurant = restaurantList[currentIndex]

                                        // Give Firebase a moment to catch up
                                        delay(50)

                                        // Update everything
                                        restaurantViewModel.refreshRestaurants(context)

                                        // Remove from our local list for immediate feedback
                                        restaurantList.remove(currentRestaurant)

                                        isCardSwiping = false
                                        refreshTrigger++
                                    } catch (e: Exception) {
                                        Log.e("HomeView", "Oops! Swipe recording failed: ${e.message}", e)
                                        isCardSwiping = false
                                    }
                                }
                            },
                            onDetailsClick = {
                                showDetailScreen = true
                            },
                            onSwipeUp = {
                                goToNextRestaurant()
                            },
                            onSwipeDown = {
                                goToPreviousRestaurant()
                            }
                        )
                    } else {
                        // All done!
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

                    // Detailed view that slides up
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
                                        isCardSwiping = true

                                        val curRestaurant = restaurantList[currentIndex]
                                        SwipeQueryable.recordSwipe(curRestaurant.id, curRestaurant.name, true)

                                        delay(10)

                                        if (currentIndex < restaurantList.size - 1) {
                                            currentIndex++
                                        }

                                        delay(10)
                                        isCardSwiping = false
                                    }
                                },
                                onDislike = {
                                    coroutineScope.launch {
                                        showDetailScreen = false
                                        isCardSwiping = true

                                        val curRestaurant = restaurantList[currentIndex]
                                        SwipeQueryable.recordSwipe(curRestaurant.id, curRestaurant.name, false)

                                        delay(10)

                                        if (currentIndex < restaurantList.size - 1) {
                                            currentIndex++
                                        }

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
                    ) { /* Consume click */ }
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
                    text = "A wonderful restaurant.",
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

    // Get the current day and time
    val calendar = Calendar.getInstance()
    // Convert Java day of week (Sunday=1, Monday=2, ..., Saturday=7) to Yelp format (Monday=0, ..., Sunday=6)
    val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

    // Current time in 24-hour format as a string (e.g., "1430" for 2:30 PM)
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTime = String.format("%02d%02d", currentHour, currentMinute)

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