package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.R
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.navigation.Screen
import com.example.swipebyte.ui.viewmodel.MyLikesViewModel
import com.example.swipebyte.ui.viewmodel.FriendViewModel
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

@Composable
fun MyLikesView(
    navController: NavController,
    userId: String,
    myLikesViewModel: MyLikesViewModel = viewModel(),
    friendViewModel: FriendViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    val likedRestaurants by myLikesViewModel.likedRestaurants.collectAsState(emptyList())
    val timestampsMap by myLikesViewModel.timestampsMap.collectAsState(emptyMap())
    var timeFilter by remember { mutableStateOf("Last 24 hours") }
    var selectedCuisines by remember { mutableStateOf(setOf<String>()) }
    var selectedCosts by remember { mutableStateOf(setOf<String>()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val repository = remember { RestaurantRepository() }

    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    // State to track which view to display (e.g., "likes" vs. "friends")
    var currentView by remember { mutableStateOf("likes") }

    // Friend-related state
    val pendingRequests by friendViewModel.pendingRequests.observeAsState(emptyList())
    val friendsList by friendViewModel.friendsList.observeAsState(emptyList())
    var emailInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Keep original values to restore if dialog is canceled
    var originalTimeFilter by remember { mutableStateOf(timeFilter) }
    var originalSelectedCuisines by remember { mutableStateOf(selectedCuisines) }
    var originalSelectedCosts by remember { mutableStateOf(selectedCosts) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Fetch user location
            val user = repository.getUserPreferences()
            userLocation = user?.location
            myLikesViewModel.fetchUserSwipedRestaurants(userId)
        } catch (e: Exception) {
            Log.e("MyLikesView", "Error fetching data: ${e.message}")
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        isLoading = true
        myLikesViewModel.fetchUserSwipedRestaurants(userId)
        isLoading = false
    }

    // Load friend data when needed
    LaunchedEffect(userId, currentView) {
        if (currentView == "friends") {
            friendViewModel.loadPendingRequests(userId)
            friendViewModel.loadFriendsList(userId)
        }
    }

    // When friend list changes, fetch friend likes from swipes
    LaunchedEffect(friendsList) {
        val friendIds = friendsList.map { it.first }
        myLikesViewModel.fetchFriendLikes(friendIds)
    }

    val oneDayMillis = 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()
    // Compute filtered list based on time, cuisine and cost filters
    val filteredRestaurants = remember(likedRestaurants, timestampsMap, timeFilter, selectedCuisines, selectedCosts) {
        likedRestaurants.filter { restaurant ->
            val restId = restaurant.id ?: return@filter false
            val ts = timestampsMap[restId] ?: 0L
            // Only include restaurants with a valid (non-zero) timestamp that was set within the last 24 hours.
            val timeCondition = if (timeFilter == "Last 24 hours") {
                ts != 0L && (currentTime - ts) < oneDayMillis
            } else true
            val cuisineCondition = if (selectedCuisines.isEmpty()) true
            else restaurant.cuisineType.any { it in selectedCuisines }
            val costCondition = if (selectedCosts.isEmpty()) true
            else restaurant.priceRange?.trim() in selectedCosts
            timeCondition && cuisineCondition && costCondition
        }
    }

    // Sort the filtered restaurants by most recent like (timestamp descending)
    val sortedFilteredRestaurants = filteredRestaurants.sortedByDescending { restaurant ->
        timestampsMap[restaurant.id] ?: 0L
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE53935), Color(0xFFEF5350))
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
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Toggle buttons for switching views
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { currentView = "likes" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentView == "likes")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("My Likes")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { currentView = "friends" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentView == "friends")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Friend Requests")
            }
        }

        // Content based on selected view
        when (currentView) {
            "likes" -> {
                // Header row with title and filter button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "My Likes", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = {
                        // Save original values when opening the dialog
                        originalTimeFilter = timeFilter
                        originalSelectedCuisines = selectedCuisines
                        originalSelectedCosts = selectedCosts
                        showFilterDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showFilterDialog) {
                    MyLikesFilterDialog(
                        timeFilter = timeFilter,
                        onTimeFilterChange = { timeFilter = it },
                        selectedCuisines = selectedCuisines,
                        onCuisinesChange = { selectedCuisines = it },
                        selectedCosts = selectedCosts,
                        onCostsChange = { selectedCosts = it },
                        onApply = { showFilterDialog = false },
                        onDismiss = {
                            // Restore original values when canceling
                            timeFilter = originalTimeFilter
                            selectedCuisines = originalSelectedCuisines
                            selectedCosts = originalSelectedCosts
                            showFilterDialog = false
                        }
                    )
                }
                // List of liked restaurants using the filtered and sorted list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        sortedFilteredRestaurants.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No liked restaurants found")
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(sortedFilteredRestaurants) { restaurant ->
                                    // Retrieve the like timestamp for this restaurant (if available)
                                    val ts = timestampsMap[restaurant.id]
                                    LikedRestaurantCard(
                                        restaurant = restaurant,
                                        likedTimestamp = ts,
                                        friendLikes = myLikesViewModel.friendLikesMap.value[restaurant.id] ?: emptyList(),
                                        userLocation = userLocation
                                    ) {
                                        selectedRestaurant = restaurant
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "friends" -> {
                // Friend Requests View Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Add Friends", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Enter Email to Add Friend") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (emailInput.isNotEmpty()) {
                                friendViewModel.sendFriendRequest(userId, emailInput)
                                emailInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Friend Request")
                    }

                    // Friend Requests Section
                    if (pendingRequests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Friend Requests (${pendingRequests.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            pendingRequests.forEach { (request, name) ->
                                FriendRequestItem(
                                    request = request,
                                    name = name,
                                    onAccept = { friendViewModel.acceptFriendRequest(request.requestId, request.senderId, userId) },
                                    onDecline = { friendViewModel.declineFriendRequest(request.requestId, userId) }
                                )
                            }
                        }
                    }

                    // Friends List Section
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Friends (${friendsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (friendsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No friends yet. Send a friend request to get started!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column {
                            friendsList.forEach { (friendId, friendName) ->
                                FriendItem(friendId = friendId, friendName = friendName)
                            }
                        }
                    }
                }
            }
        }
    }

    // Show restaurant info in a dialog when a restaurant is selected
    if (selectedRestaurant != null) {
        Dialog(
            onDismissRequest = { selectedRestaurant = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            RestaurantInfoScreen(
                restaurant = selectedRestaurant!!,
                onDismiss = { selectedRestaurant = null }
            )
        }
    }
}

@Composable
fun MyLikesFilterDialog(
    timeFilter: String,
    onTimeFilterChange: (String) -> Unit,
    selectedCuisines: Set<String>,
    onCuisinesChange: (Set<String>) -> Unit,
    selectedCosts: Set<String>,
    onCostsChange: (Set<String>) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter My Likes") },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text("Time Filter", style = MaterialTheme.typography.titleMedium)
                    val timeOptions = listOf("Last 24 hours", "All Time")
                    timeOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTimeFilterChange(option) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (timeFilter == option),
                                onClick = { onTimeFilterChange(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cuisine Preferences", style = MaterialTheme.typography.titleMedium)
                    val allCuisines = listOf("Italian", "Chinese", "Mexican", "Indian", "American", "Japanese", "Thai")
                    allCuisines.forEach { cuisine ->
                        val isSelected = cuisine in selectedCuisines
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedCuisines.toMutableSet()
                                    if (isSelected) newSet.remove(cuisine) else newSet.add(cuisine)
                                    onCuisinesChange(newSet)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newSet = selectedCuisines.toMutableSet()
                                    if (checked) newSet.add(cuisine) else newSet.remove(cuisine)
                                    onCuisinesChange(newSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cuisine)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cost Preferences", style = MaterialTheme.typography.titleMedium)
                    val costOptions = listOf("$", "$$", "$$$", "$$$$")
                    costOptions.forEach { cost ->
                        val isSelected = cost in selectedCosts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedCosts.toMutableSet()
                                    if (isSelected) newSet.remove(cost) else newSet.add(cost)
                                    onCostsChange(newSet)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newSet = selectedCosts.toMutableSet()
                                    if (checked) newSet.add(cost) else newSet.remove(cost)
                                    onCostsChange(newSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cost)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LikedRestaurantCard(
    restaurant: Restaurant,
    likedTimestamp: Long?, // like timestamp badge remains
    friendLikes: List<String>, // new parameter for friend likes
    userLocation: GeoPoint?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (restaurant.imageUrls.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = restaurant.imageUrls.first()),
                        contentDescription = restaurant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Overlay for darkening the image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                // Badge for like timestamp (top right)
                likedTimestamp?.let { ts ->
                    val currentTime = System.currentTimeMillis()
                    val diffMillis = currentTime - ts
                    val diffMinutes = diffMillis / (60 * 1000)
                    val badgeText = when {
                        diffMinutes < 60 -> "liked ${if (diffMinutes < 1) 1 else diffMinutes} min ago"
                        diffMinutes < 1440 -> {
                            val diffHours = diffMinutes / 60
                            "liked ${if (diffHours < 1) 1 else diffHours} hour${if (diffHours > 1) "s" else ""} ago"
                        }
                        else -> {
                            val diffDays = diffMinutes / 1440
                            "liked ${if (diffDays < 1) 1 else diffDays} day${if (diffDays > 1) "s" else ""} ago"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                // New badge for friend likes at the bottom right
                if (friendLikes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${friendLikes.size} friend${if (friendLikes.size > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                // Restaurant name at the bottom left
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            // Additional details below the image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.cuisineType.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = String.format(Locale.US, "⭐%.1f", restaurant.yelpRating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = calculateDistance(userLocation, restaurant.location),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

fun calculateDistance(userLocation: GeoPoint?, restaurantLocation: GeoPoint?): String {
    if (userLocation == null || restaurantLocation == null) return "Distance unknown"

    val userLat = userLocation.latitude
    val userLng = userLocation.longitude
    val restLat = restaurantLocation.latitude
    val restLng = restaurantLocation.longitude

    val results = FloatArray(1)
    android.location.Location.distanceBetween(userLat, userLng, restLat, restLng, results)
    val distanceKm = results[0] / 1000.0  // Convert meters to kilometers

    return String.format(Locale.US, "%.2f km away", distanceKm)
}
