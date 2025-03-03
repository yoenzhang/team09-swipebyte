package com.example.swipebyte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.navigation.NavController
import com.example.swipebyte.ui.db.Restaurant

@Composable
fun CommunityFavouritesView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "🌟 Community Favorites",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilterOptions()

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            val demoRestaurants = listOf(
                Restaurant("Badiali Pizzeria", "Pizza", "4.5 ⭐ (900+)", "0.3 km away", "https://images.unsplash.com/photo-1601924575440-30dc45f7d24b"),
                Restaurant("Bar Poet", "Bar, Pizza", "4.6 ⭐ (1,000+)", "0.4 km away", "https://images.unsplash.com/photo-1559628233-93f092d3abfa"),
                Restaurant("Sugo", "Italian", "4.7 ⭐ (2,000+)", "0.6 km away", "https://images.unsplash.com/photo-1603079837747-7e0663f64d1e"),
                Restaurant("Joe's Diner", "American", "4.3 ⭐ (500+)", "0.7 km away", "https://images.unsplash.com/photo-1551218808-94e220e084d2"),
                Restaurant("Green Bowl", "Vegan", "4.8 ⭐ (300+)", "0.9 km away", "https://images.unsplash.com/photo-1490645935967-10de6ba17061"),
                Restaurant("Taco Town", "Mexican", "4.5 ⭐ (700+)", "1.1 km away", "https://images.unsplash.com/photo-1599999901341-b3d8429a7512"),
                Restaurant("Sushi World", "Japanese", "4.9 ⭐ (1,500+)", "1.3 km away", "https://images.unsplash.com/photo-1594007654731-d7430cdc24ca"),
                Restaurant("Burger Haven", "Burgers", "4.4 ⭐ (800+)", "1.5 km away", "https://images.unsplash.com/photo-1550547660-d9450f859349"),
                Restaurant("Pasta Palace", "Italian", "4.6 ⭐ (600+)", "1.7 km away", "https://images.unsplash.com/photo-1525755662778-989d0524087e"),
                Restaurant("Dessert Dreams", "Desserts", "4.7 ⭐ (900+)", "2.0 km away", "https://images.unsplash.com/photo-1599785209707-26dc3e3cc094")
            )

            items(demoRestaurants) { restaurant ->
                CommunityFavouriteCard(
                    name = restaurant.name,
                    cuisine = restaurant.cuisine,
                    rating = restaurant.rating,
                    distance = restaurant.distance,
                    imageUrl = restaurant.imageUrl
                )
            }
        }
    }
}

@Composable
fun CommunityFavouriteCard(
    name: String,
    cuisine: String,
    rating: String,
    distance: String,
    imageUrl: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Light orange background
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "Place Image",
                modifier = Modifier
                    .size(100.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cuisine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = rating,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = distance,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}