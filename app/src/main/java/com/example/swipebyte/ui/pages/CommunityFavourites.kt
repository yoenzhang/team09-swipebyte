package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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


@Composable
fun CommunityFavouritesView(navController: NavController, viewModel: CommunityFavouritesViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteRestaurants by viewModel.favorites.collectAsState(initial = emptyList())
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    val repository = remember { RestaurantRepository() }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(Unit) {
        try {
            val user = repository.getUserPreferences()
            userLocation = user?.location
            viewModel.firebaseSwipeListener(userLocation)
        } catch (e: Exception) {
            Log.e("CommunityFavourites", "Error fetching community favorites: ${e.message}")
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
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Community Favorites",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (favoriteRestaurants.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No community favorites found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(favoriteRestaurants) { restaurant ->
                        CommunityFavouriteCard(
                            restaurant = restaurant,
                            onClick = { selectedRestaurant = restaurant}
                        )
                    }
                }
            }
        }
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