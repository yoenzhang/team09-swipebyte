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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.R
import com.example.swipebyte.ui.data.models.Restaurant
import com.example.swipebyte.ui.viewmodel.CommunityFavouritesViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.swipebyte.data.repository.RestaurantRepository
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

// Class for Memento design pattern
data class FilterMemento(
    val timeFilter: String,
    val selectedCuisines: Set<String>,
    val selectedCosts: Set<String>
)

@Composable
fun CommunityFavouritesView(navController: NavController, viewModel: CommunityFavouritesViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteRestaurants by viewModel.favorites.collectAsState(initial = emptyList())
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    val repository = remember { RestaurantRepository() }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter states
    var timeFilter by remember { mutableStateOf("All Time") }
    var selectedCuisines by remember { mutableStateOf(setOf<String>()) }
    var selectedCosts by remember { mutableStateOf(setOf<String>()) }

    // Original filter states to restore if canceled
    var originalTimeFilter by remember { mutableStateOf(timeFilter) }
    var originalSelectedCuisines by remember { mutableStateOf(selectedCuisines) }
    var originalSelectedCosts by remember { mutableStateOf(selectedCosts) }

    LaunchedEffect(Unit) {
        try {
            val user = repository.getUserPreferences()
            userLocation = user?.location
            viewModel.firebaseSwipeListener(userLocation)
        } catch (e: Exception) {
            Log.e("CommunityFavourites", "Error fetching community favorites: ${e.message}")
        }
    }

    // Apply filters to the restaurant list
    val filteredRestaurants = remember(favoriteRestaurants, timeFilter, selectedCuisines, selectedCosts) {
        favoriteRestaurants.filter { restaurant ->
            val cuisineCondition = if (selectedCuisines.isEmpty()) true
            else restaurant.cuisineType.any { it in selectedCuisines }

            val costCondition = if (selectedCosts.isEmpty()) true
            else restaurant.priceRange?.trim() in selectedCosts

            cuisineCondition && costCondition
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
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
            }
        }

        // Header row with title and filter button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Community Favorites",
                style = MaterialTheme.typography.headlineMedium
            )
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

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredRestaurants.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No community favorites found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredRestaurants) { restaurant ->
                        CommunityFavouriteCard(
                            restaurant = restaurant,
                            onClick = { selectedRestaurant = restaurant }
                        )
                    }
                }
            }
        }
    }

    // Show filter dialog
    if (showFilterDialog) {
        CommunityFavouritesFilterDialog(
            timeFilter = timeFilter,
            onTimeFilterChange = { timeFilter = it },
            selectedCuisines = selectedCuisines,
            onCuisinesChange = { selectedCuisines = it },
            selectedCosts = selectedCosts,
            onCostsChange = { selectedCosts = it },
            onApply = {
                // Just close the dialog, changes are already applied
                showFilterDialog = false
            },
            onDismiss = {
                // Restore original values when canceling
                timeFilter = originalTimeFilter
                selectedCuisines = originalSelectedCuisines
                selectedCosts = originalSelectedCosts
                showFilterDialog = false
            }
        )
    }

    // Show restaurant info dialog when a restaurant is selected
    if (selectedRestaurant != null) {
        Dialog(
            onDismissRequest = { selectedRestaurant = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            RestaurantInfoScreen(restaurant = selectedRestaurant!!, onDismiss = { selectedRestaurant = null })
        }
    }
}

@Composable
fun CommunityFavouritesFilterDialog(
    timeFilter: String,
    onTimeFilterChange: (String) -> Unit,
    selectedCuisines: Set<String>,
    onCuisinesChange: (Set<String>) -> Unit,
    selectedCosts: Set<String>,
    onCostsChange: (Set<String>) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    // Memento stack
    var history by remember { mutableStateOf(listOf(FilterMemento(timeFilter, selectedCuisines, selectedCosts))) }
    var historyIndex by remember { mutableIntStateOf(0) }

    fun undo() {
        historyIndex--
        val previousState = history[historyIndex]
        onTimeFilterChange(previousState.timeFilter)
        onCuisinesChange(previousState.selectedCuisines)
        onCostsChange(previousState.selectedCosts)
    }

    fun redo() {
        historyIndex++
        val nextState = history[historyIndex]
        onTimeFilterChange(nextState.timeFilter)
        onCuisinesChange(nextState.selectedCuisines)
        onCostsChange(nextState.selectedCosts)
    }

    fun saveState(newTimeFilter: String = timeFilter, newSelectedCuisines: Set<String> = selectedCuisines,
                  newSelectedCosts: Set<String> = selectedCosts) {
        val newState = FilterMemento(newTimeFilter, newSelectedCuisines, newSelectedCosts)

        // Trim redo history if new change happens after undo
        if (historyIndex < history.size - 1) {
            history = history.subList(0, historyIndex + 1)
        }

        history = history + newState
        historyIndex = history.size - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Community Favorites") },
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
                                .clickable { onTimeFilterChange(option)
                                             saveState(newTimeFilter = option) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (timeFilter == option),
                                onClick = { onTimeFilterChange(option)
                                            saveState(newTimeFilter = option) }
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
                                    saveState(newSelectedCuisines = newSet)
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
                                    saveState(newSelectedCuisines = newSet)
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
                                    saveState(newSelectedCosts = newSet)
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
                                    saveState(newSelectedCosts = newSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cost)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {undo()},
                            modifier = Modifier.weight(1f),
                            enabled = historyIndex > 0
                        ) {
                            Text("Undo")
                        }
                        Button(
                            onClick = {redo()},
                            modifier = Modifier.weight(1f),
                            enabled = historyIndex < history.size - 1
                        ) {
                            Text("Redo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row for Cancel/Apply, full width buttons
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
                            onClick = onApply,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun CommunityFavouriteCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with full-width image and overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = restaurant.imageUrls.first()),
                    contentDescription = "Place Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Content section with additional details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.cuisineType.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = String.format(Locale.US, "⭐%.1f", restaurant.yelpRating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f km away", restaurant.distance),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = String.format(Locale.US, "%s%d", if (restaurant.voteCount >= 0) "👍" else "👎", restaurant.voteCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}