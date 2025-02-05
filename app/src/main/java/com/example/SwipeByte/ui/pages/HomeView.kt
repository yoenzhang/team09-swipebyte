package com.example.SwipeByte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

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
    val restaurantList = listOf(
        Restaurant("Lazeez Shawarma", "Middle Eastern - $$$$", "2.6 ★ (1,000+)", "1.5 km away",
            "https://d1ralsognjng37.cloudfront.net/83bb5d98-43b6-4b86-bb9e-4c4c89c11a33.jpeg"),
        Restaurant("Subway", "Fast Food - $$", "3.8 ★ (2,000+)", "800m away",
            "https://www.subway.com/ns/images/hero/Sandwich_Buffet.jpg"),
        Restaurant("McDonald's", "Burgers - $$", "4.1 ★ (5,000+)", "500m away",
            "https://www.mcdonalds.com/content/dam/sites/usa/nfl/publication/1PUB_106_McD_Top20WebsiteRefresh_Photos_QuarterPounderCheese.jpg"),
        Restaurant("Pizza Hut", "Italian - $$$", "4.0 ★ (3,500+)", "2.0 km away",
            "https://www.pizzahut.com/assets/w/tile/th-menu-icon.jpg")
    )

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
