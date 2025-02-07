package com.example.swipebyte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.swipebyte.ui.db.DBModel
import com.google.accompanist.pager.*

data class Restaurant(
    val name: String,
    val cuisine: String,
    val rating: String,
    val distance: String,
    val imageUrl: String
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeView(navController: NavController) {
    val restaurantList = remember { mutableStateListOf<Restaurant>() }

    // Fetch data once when the composable is first launched
    LaunchedEffect(Unit) {
        val fetchedRestaurants = DBModel.fetchRestaurants()
        restaurantList.addAll(fetchedRestaurants)
    }

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

@Composable
fun RestaurantCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier
            .fillMaxSize() // ✅ Makes each card fill the screen
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberImagePainter(restaurant.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f) // ✅ 50% screen height for the image
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f) // ✅ 50% screen height for text
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = restaurant.name,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = restaurant.cuisine,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = restaurant.rating,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = restaurant.distance,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
