package com.example.swipebyte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.R
import com.example.swipebyte.ui.navigation.Screen
import com.example.swipebyte.ui.viewmodel.MyLikesViewModel

@Composable
fun MyLikesView(
    navController: NavController,
    userId: String,  // Pass the user's Firestore userId
    myLikesViewModel: MyLikesViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    val likedRestaurants by myLikesViewModel.likedRestaurants.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        isLoading = true
        myLikesViewModel.fetchUserSwipedRestaurants(userId)
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
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
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate(Screen.ProfileSettings.route) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Profile Settings"
                    )
                }
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
                text = "💖 My Likes",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    // Show a loading indicator while we fetch data
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                likedRestaurants.isEmpty() -> {
                    // If no restaurants found for this user
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No restaurants found")
                    }
                }
                else -> {
                    // Show the list of restaurants
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(likedRestaurants) { restaurant ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (restaurant.imageUrls.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = restaurant.imageUrls.first()),
                                            contentDescription = restaurant.name,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Column {
                                        Text(
                                            text = restaurant.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = restaurant.cuisineType.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Rating: ${restaurant.averageRating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
