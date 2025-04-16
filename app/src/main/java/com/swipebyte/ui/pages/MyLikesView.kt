package com.swipebyte.ui.pages

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.swipebyte.R
import com.swipebyte.data.repository.RestaurantRepository
import com.swipebyte.ui.data.models.Restaurant
import com.swipebyte.ui.navigation.Screen
import com.swipebyte.ui.viewmodel.MyLikesViewModel
import com.swipebyte.ui.viewmodel.FriendViewModel
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

data class FriendFilterMemento(
    val timeFilter: String,
    val selectedCuisines: Set<String>,
    val selectedCosts: Set<String>,
    val selectedFriends: Set<String>,
    val showOnlyFavourites: Boolean
)

@Composable
fun MyLikesView(
    navController: NavController,
    userId: String,
    myLikesViewModel: MyLikesViewModel = viewModel(key = "MyLikesViewModel"),
    friendViewModel: FriendViewModel = viewModel() // (add key here as needed)
) {
    // Use Flow state for loading and data
    val isLoading by myLikesViewModel.isLoading.collectAsState()
    val likedRestaurants by myLikesViewModel.likedRestaurants.collectAsState(emptyList())
    val timestampsMap by myLikesViewModel.timestampsMap.collectAsState(emptyMap())
    val favouritesMap by myLikesViewModel.favouritesMap.collectAsState(emptySet())

    // Use rememberSaveable so that these filter preferences persist across navigation
    var timeFilter by rememberSaveable { mutableStateOf("Last 24 hours") }
    var selectedCuisines by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedCosts by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedFriends by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showOnlyFavourites by rememberSaveable { mutableStateOf(false) }

    // Save originals to restore if dialog is cancelled
    var originalTimeFilter = timeFilter
    var originalSelectedCuisines = selectedCuisines
    var originalSelectedCosts = selectedCosts
    var originalSelectedFriends = selectedFriends
    var originalShowOnlyFavourites = showOnlyFavourites

    var showFilterDialog by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val repository = remember { RestaurantRepository() }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var currentView by remember { mutableStateOf("likes") }

    // Friend-related state
    val pendingRequests by friendViewModel.pendingRequests.observeAsState(emptyList())
    val friendsList by friendViewModel.friendsList.observeAsState(emptyList())
    var emailInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val operationResult by friendViewModel.operationResult.observeAsState()

    // Fetch data and start the real‑time snapshot listener for likes
    LaunchedEffect(userId) {
        try {
            val user = repository.getUserPreferences()
            userLocation = user?.location
            myLikesViewModel.firebaseSwipeListenerForLikes(userId, userLocation)
        } catch (e: Exception) {
            Log.e("MyLikesView", "Error fetching data: ${e.message}")
        }
    }
    LaunchedEffect(userId) {
        friendViewModel.loadFriendsList(userId)
    }
    LaunchedEffect(userId, currentView) {
        if (currentView == "friends") {
            friendViewModel.loadPendingRequests(userId)
        }
    }
    LaunchedEffect(friendsList) {
        val friendIds = friendsList.map { it.first }
        myLikesViewModel.fetchFriendLikes(friendIds)
    }
    LaunchedEffect(operationResult) {
        operationResult?.let { result ->
            val message = result.getOrNull()?.takeIf { it.isNotBlank() }
                ?: result.exceptionOrNull()?.message ?: (if (result.isSuccess) "Operation successful!" else "Operation failed.")
            if (message.isNotBlank() && message != "Operation successful!" && message != "Operation failed.") {
                snackbarHostState.showSnackbar(message)
            }
            friendViewModel.clearOperationResult()
        }
    }

    // FILTER + SORT logic remains the same
    val oneDayMillis = 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()
    val filteredRestaurants = remember(
        likedRestaurants,
        timestampsMap,
        timeFilter,
        selectedCuisines,
        selectedCosts,
        selectedFriends,
        showOnlyFavourites,
        favouritesMap
    ) {
        likedRestaurants.filter { restaurant ->
            val restId = restaurant.id
            val ts = timestampsMap[restId] ?: 0L
            val timeCondition = if (timeFilter == "Last 24 hours") {
                ts != 0L && (currentTime - ts) < oneDayMillis
            } else true
            val cuisineCondition = if (selectedCuisines.isEmpty()) true else restaurant.cuisineType.any { it in selectedCuisines }
            val costCondition = if (selectedCosts.isEmpty()) true else restaurant.priceRange?.trim() in selectedCosts
            val friendLikesList = myLikesViewModel.friendLikesMap.value[restId] ?: emptyList()
            val friendCondition = if (selectedFriends.isEmpty()) true else friendLikesList.any { it in selectedFriends }
            val favouritesCondition = if (showOnlyFavourites) {
                val favourites = myLikesViewModel.favouritesMap.value
                restId in favourites
            } else true

            timeCondition && cuisineCondition && costCondition && friendCondition && favouritesCondition
        }
    }

    val sortedFilteredRestaurants = remember(filteredRestaurants, selectedFriends) {
        if (selectedFriends.isNotEmpty()) {
            filteredRestaurants.sortedWith(
                compareByDescending<Restaurant> { restaurant ->
                    val whoLiked = myLikesViewModel.friendLikesMap.value[restaurant.id] ?: emptyList()
                    whoLiked.count { it in selectedFriends }
                }.thenByDescending { restaurant ->
                    timestampsMap[restaurant.id] ?: 0L
                }
            )
        } else {
            filteredRestaurants.sortedByDescending { restaurant ->
                timestampsMap[restaurant.id] ?: 0L
            }
        }
    }

    LaunchedEffect(showFilterDialog) {
        if (showFilterDialog) {
            myLikesViewModel.refreshFavouritesStatus()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top app bar
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
            // Toggle buttons
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
                ) { Text("My Likes") }
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
                ) { Text("Friend Requests") }
            }
            // Main content based on selected view
            when (currentView) {
                "likes" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "My Likes",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row {
                            IconButton(onClick = {
                                myLikesViewModel.firebaseSwipeListenerForLikes(userId, userLocation)
                                val friendIds = friendsList.map { it.first }
                                myLikesViewModel.fetchFriendLikes(friendIds)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                originalTimeFilter = timeFilter
                                originalSelectedCuisines = selectedCuisines
                                originalSelectedCosts = selectedCosts
                                originalSelectedFriends = selectedFriends
                                originalShowOnlyFavourites = showOnlyFavourites
                                myLikesViewModel.refreshFavouritesStatus()
                                showFilterDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (showFilterDialog) {
                        MyLikesFilterDialog(
                            timeFilter = timeFilter,
                            onTimeFilterChange = { timeFilter = it },
                            selectedCuisines = selectedCuisines.toSet(),
                            onCuisinesChange = { selectedCuisines = it.toList() },
                            selectedCosts = selectedCosts.toSet(),
                            onCostsChange = { selectedCosts = it.toList() },
                            allFriendNames = friendsList.map { it.second },
                            selectedFriends = selectedFriends.toSet(),
                            onFriendsChange = { selectedFriends = it.toList() },
                            showOnlyFavourites = showOnlyFavourites,
                            onShowOnlyFavouritesChange = { showOnlyFavourites = it },
                            onApply = { showFilterDialog = false },
                            onDismiss = {
                                timeFilter = originalTimeFilter
                                selectedCuisines = originalSelectedCuisines
                                selectedCosts = originalSelectedCosts
                                selectedFriends = originalSelectedFriends
                                showOnlyFavourites = originalShowOnlyFavourites
                                showFilterDialog = false
                            },
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            if (likedRestaurants.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("You have not liked any restaurants")
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(sortedFilteredRestaurants) { restaurant ->
                                        val ts = timestampsMap[restaurant.id]
                                        LikedRestaurantCard(
                                            restaurant = restaurant,
                                            likedTimestamp = ts,
                                            friendLikes = if (selectedFriends.isEmpty()) emptyList()
                                            else myLikesViewModel.friendLikesMap.value[restaurant.id]
                                                ?.filter { it in selectedFriends } ?: emptyList(),
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
                            shape = RoundedCornerShape(10.dp)
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
                                        onAccept = {
                                            friendViewModel.acceptFriendRequest(
                                                request.requestId,
                                                request.senderId,
                                                userId
                                            )
                                        },
                                        onDecline = {
                                            friendViewModel.declineFriendRequest(
                                                request.requestId,
                                                userId
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Friends (${friendsList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (friendsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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
    }
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
    allFriendNames: List<String>,
    selectedFriends: Set<String>,
    onFriendsChange: (Set<String>) -> Unit,
    showOnlyFavourites: Boolean,
    onShowOnlyFavouritesChange: (Boolean) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var localTimeFilter by remember(timeFilter) { mutableStateOf(timeFilter) }
    var localSelectedCuisines by remember(selectedCuisines) { mutableStateOf(selectedCuisines) }
    var localSelectedCosts by remember(selectedCosts) { mutableStateOf(selectedCosts) }
    var localSelectedFriends by remember(selectedFriends) { mutableStateOf(selectedFriends) }
    var localShowOnlyFavourites by remember(showOnlyFavourites) { mutableStateOf(showOnlyFavourites) }

    // History for undo/redo
    var history by remember { mutableStateOf(listOf(FriendFilterMemento(localTimeFilter, localSelectedCuisines, localSelectedCosts, localSelectedFriends, localShowOnlyFavourites))) }
    var historyIndex by remember { mutableIntStateOf(0) }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val previousState = history[historyIndex]
            localTimeFilter = previousState.timeFilter
            localSelectedCuisines = previousState.selectedCuisines
            localSelectedCosts = previousState.selectedCosts
            localSelectedFriends = previousState.selectedFriends
            localShowOnlyFavourites = previousState.showOnlyFavourites
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val nextState = history[historyIndex]
            localTimeFilter = nextState.timeFilter
            localSelectedCuisines = nextState.selectedCuisines
            localSelectedCosts = nextState.selectedCosts
            localSelectedFriends = nextState.selectedFriends
            localShowOnlyFavourites = nextState.showOnlyFavourites
        }
    }

    fun saveState(
        newTimeFilter: String = localTimeFilter,
        newSelectedCuisines: Set<String> = localSelectedCuisines,
        newSelectedCosts: Set<String> = localSelectedCosts,
        newSelectedFriends: Set<String> = localSelectedFriends,
        newShowOnlyFavourites: Boolean = localShowOnlyFavourites
    ) {
        val newState = FriendFilterMemento(newTimeFilter, newSelectedCuisines, newSelectedCosts, newSelectedFriends, newShowOnlyFavourites)
        // Trim any redo history if new change happens after undo
        if (historyIndex < history.size - 1) {
            history = history.subList(0, historyIndex + 1)
        }
        history = history + newState
        historyIndex = history.size - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter My Likes") },
        text = {
            // The overall content is a fixed Column:
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                // Scrollable area for filter options
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Favourites Filter
                        Text("Favourites Filter", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localShowOnlyFavourites = !localShowOnlyFavourites
                                    saveState(newShowOnlyFavourites = localShowOnlyFavourites)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = localShowOnlyFavourites,
                                onCheckedChange = { checked ->
                                    localShowOnlyFavourites = checked
                                    saveState(newShowOnlyFavourites = checked)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show only favourited restaurants")
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Time Filter
                        Text("Time Filter", style = MaterialTheme.typography.titleMedium)
                        val timeOptions = listOf("Last 24 hours", "All Time")
                        timeOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        localTimeFilter = option
                                        saveState(newTimeFilter = option)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (localTimeFilter == option),
                                    onClick = {
                                        localTimeFilter = option
                                        saveState(newTimeFilter = option)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Cuisine Preferences
                        Text("Cuisine Preferences", style = MaterialTheme.typography.titleMedium)
                        val allCuisines = listOf("Italian", "Chinese", "Mexican", "Indian", "American", "Japanese", "Thai")
                        allCuisines.forEach { cuisine ->
                            val isSelected = cuisine in localSelectedCuisines
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = localSelectedCuisines.toMutableSet()
                                        if (isSelected) newSet.remove(cuisine) else newSet.add(cuisine)
                                        localSelectedCuisines = newSet
                                        saveState(newSelectedCuisines = newSet)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        val newSet = localSelectedCuisines.toMutableSet()
                                        if (checked) newSet.add(cuisine) else newSet.remove(cuisine)
                                        localSelectedCuisines = newSet
                                        saveState(newSelectedCuisines = newSet)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cuisine)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Cost Preferences
                        Text("Cost Preferences", style = MaterialTheme.typography.titleMedium)
                        val costOptions = listOf("$", "$$", "$$$", "$$$$")
                        costOptions.forEach { cost ->
                            val isSelected = cost in localSelectedCosts
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = localSelectedCosts.toMutableSet()
                                        if (isSelected) newSet.remove(cost) else newSet.add(cost)
                                        localSelectedCosts = newSet
                                        saveState(newSelectedCosts = newSet)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        val newSet = localSelectedCosts.toMutableSet()
                                        if (checked) newSet.add(cost) else newSet.remove(cost)
                                        localSelectedCosts = newSet
                                        saveState(newSelectedCosts = newSet)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cost)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Friend Filter
                        Text("Friend Filter", style = MaterialTheme.typography.titleMedium)
                        if (allFriendNames.isEmpty()) {
                            Text("No friends found.")
                        } else {
                            allFriendNames.forEach { friendName ->
                                val isSelected = friendName in localSelectedFriends
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = localSelectedFriends.toMutableSet()
                                            if (isSelected) newSet.remove(friendName) else newSet.add(friendName)
                                            localSelectedFriends = newSet
                                            saveState(newSelectedFriends = newSet)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            val newSet = localSelectedFriends.toMutableSet()
                                            if (checked) newSet.add(friendName) else newSet.remove(friendName)
                                            localSelectedFriends = newSet
                                            saveState(newSelectedFriends = newSet)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(friendName)
                                }
                            }
                        }
                    }
                }
                // Fixed undo/redo row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { undo() },
                        modifier = Modifier.weight(1f),
                        enabled = historyIndex > 0
                    ) {
                        Text("Undo")
                    }
                    Button(
                        onClick = { redo() },
                        modifier = Modifier.weight(1f),
                        enabled = historyIndex < history.size - 1
                    ) {
                        Text("Redo")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Fixed Cancel and Apply row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onTimeFilterChange(localTimeFilter)
                            onCuisinesChange(localSelectedCuisines)
                            onCostsChange(localSelectedCosts)
                            onFriendsChange(localSelectedFriends)
                            onShowOnlyFavouritesChange(localShowOnlyFavourites)
                            onApply()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun LikedRestaurantCard(
    restaurant: Restaurant,
    likedTimestamp: Long?,
    friendLikes: List<String>,
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
            // Image header
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
                // Dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                // Badge for the like timestamp
                likedTimestamp?.let { ts ->
                    val currentTime = System.currentTimeMillis()
                    val diffMillis = currentTime - ts
                    val diffMinutes = diffMillis / (60 * 1000)
                    val badgeText = when {
                        diffMinutes < 60 ->
                            "liked ${if (diffMinutes < 1) 1 else diffMinutes} min ago"
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
            // Content below the image
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
                    Spacer(modifier = Modifier.weight(1f))
                    if (friendLikes.isNotEmpty()) {
                        val friendNamesString = friendLikes.joinToString(", ")
                        Text(
                            text = "Liked by: $friendNamesString",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
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
    val distanceKm = results[0] / 1000.0
    return String.format(Locale.US, "%.2f km away", distanceKm)
}
