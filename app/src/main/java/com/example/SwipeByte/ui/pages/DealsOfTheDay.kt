package com.example.SwipeByte.ui.pages

import com.example.SwipeByte.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.SwipeByte.navigation.Screen

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
            items(10) { index -> // Replace with your actual data list
                DealCard()
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
fun DealCard() {
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
                painter = painterResource(id = R.drawable.pizza), // Replace with actual image
                contentDescription = "Deal Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Badiali Pizzeria",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Pizza - $",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "4.5 ⭐ (900+)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "0.3 km away",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}