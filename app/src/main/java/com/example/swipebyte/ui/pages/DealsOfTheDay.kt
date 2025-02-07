package com.example.swipebyte.ui.pages

import com.example.swipebyte.R
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController

@Composable
fun DealsOfTheDayView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "🔥 Deals of the Day",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilterOptions()

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val demoDeals = listOf(
                Deal("Badiali Pizzeria", "Pizza - $", "4.5 ⭐ (900+)", "0.3 km away", "https://images.unsplash.com/photo-1601924575440-30dc45f7d24b"),
                Deal("Bar Poet", "Bar, Pizza - $$", "4.6 ⭐ (1,000+)", "0.4 km away", "https://images.unsplash.com/photo-1559628233-93f092d3abfa"),
                Deal("Sugo", "Italian - $$", "4.7 ⭐ (2,000+)", "0.6 km away", "https://images.unsplash.com/photo-1603079837747-7e0663f64d1e"),
                Deal("Joe's Diner", "American - $", "4.3 ⭐ (500+)", "0.7 km away", "https://images.unsplash.com/photo-1551218808-94e220e084d2"),
                Deal("Green Bowl", "Vegan - $$", "4.8 ⭐ (300+)", "0.9 km away", "https://images.unsplash.com/photo-1490645935967-10de6ba17061"),
                Deal("Taco Town", "Mexican - $$", "4.5 ⭐ (700+)", "1.1 km away", "https://images.unsplash.com/photo-1599999901341-b3d8429a7512"),
                Deal("Sushi World", "Japanese - $$$", "4.9 ⭐ (1,500+)", "1.3 km away", "https://images.unsplash.com/photo-1594007654731-d7430cdc24ca"),
                Deal("Burger Haven", "Burgers - $", "4.4 ⭐ (800+)", "1.5 km away", "https://images.unsplash.com/photo-1550547660-d9450f859349"),
                Deal("Pasta Palace", "Italian - $$", "4.6 ⭐ (600+)", "1.7 km away", "https://images.unsplash.com/photo-1525755662778-989d0524087e"),
                Deal("Dessert Dreams", "Desserts - $$", "4.7 ⭐ (900+)", "2.0 km away", "https://images.unsplash.com/photo-1599785209707-26dc3e3cc094")
            )

            items(demoDeals) { deal ->
                DealCard(
                    name = deal.name,
                    category = deal.category,
                    rating = deal.rating,
                    distance = deal.distance,
                    imageUrl = deal.imageUrl
                )
            }
        }
    }
}

@Composable
fun FilterOptions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { /* TODO: Handle filter */ }, colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray
        )) {
            Text(text = "Distance")
        }
        Button(onClick = { /* TODO: Handle filter */ }, colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray
        )) {
            Text(text = "Price")
        }
        Button(onClick = { /* TODO: Handle filter */ }, colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray
        )) {
            Text(text = "Sort")
        }
    }
}

@Composable
fun DealCard(
    name: String,
    category: String,
    rating: String,
    distance: String,
    imageUrl: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Square cards
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "Deal Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category,
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

data class Deal(
    val name: String,
    val category: String,
    val rating: String,
    val distance: String,
    val imageUrl: String
)
