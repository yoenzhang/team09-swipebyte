package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.data.repository.RestaurantRepository
import com.example.swipebyte.ui.db.models.Restaurant
import com.google.accompanist.pager.*

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeView(navController: NavController) {
    // Initialize RestaurantRepository
    val restaurantRepo = RestaurantRepository()

    // State for the list of restaurants
    val restaurantList = remember { mutableStateListOf<Restaurant>() }

    // Loading state for when data is being fetched
    var isLoading by remember { mutableStateOf(true) }

    // Fetch data once when the composable is first launched
    LaunchedEffect(Unit) {
        try {
            // Call fetchRestaurants() to get data from RestaurantRepository
            val fetchedRestaurants = restaurantRepo.getRestaurants()
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

    // If the data is still loading, show a loading indicator
    if (isLoading) {
        //CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    } else {
        val pagerState = rememberPagerState()

        Column(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                state = pagerState, // ✅ Enables vertical swiping
                count = restaurantList.size,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                RestaurantCard(restaurantList[page])
            }
        }
    }
}


@Composable
fun RestaurantCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image (Full card)
            Image(
                painter = rememberAsyncImagePainter(model = restaurant.imageUrls[0]),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Crop
            )

            // Red Overlay at the bottom (15% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    .height((LocalConfiguration.current.screenHeightDp * 0.15f).dp) // ✅ 15% of screen height
            )

            // Text Content over the Red Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart) // Align to bottom
                    .padding(16.dp)
            ) {
                Text(
                    text = restaurant.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = restaurant.cuisineType.toString(),
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = restaurant.yelpRating.toString(),
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "1.8km away",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
