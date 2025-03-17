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
import com.example.swipebyte.ui.pages.RestaurantInfoScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CommunityFavouritesView(navController: NavController, viewModel: CommunityFavouritesViewModel = viewModel()) {
    var isLoading by remember { mutableStateOf(true) }
    val favoriteRestaurants by viewModel.favorites.collectAsState(initial = emptyList())
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter states
    var timeFilter by remember { mutableStateOf("Last 24 hours") }
    var selectedCuisines by remember { mutableStateOf(setOf<String>()) }
    var selectedCosts by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        try {
            viewModel.fetchFavorites() // Trigger fetch of community favorites
        } catch (e: Exception) {
            Log.e("CommunityFavourites", "Error fetching community favorites: ${e.message}")
        } finally {
            isLoading = false
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
            IconButton(onClick = { showFilterDialog = true }) {
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
            onApply = { showFilterDialog = false },
            onDismiss = { showFilterDialog = false }
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
                        text = "⭐ ${restaurant.averageRating}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${restaurant.distance} km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}