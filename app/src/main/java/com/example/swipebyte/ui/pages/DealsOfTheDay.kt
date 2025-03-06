package com.example.swipebyte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.ui.db.repository.DailyDeal
import com.example.swipebyte.ui.db.repository.DailyDealsRepository
import kotlinx.coroutines.delay

@Composable
fun DealsOfTheDayView(navController: NavController) {
    // Create an instance of DailyDealsRepository
    val dailyDealsRepo = DailyDealsRepository()
    // State holding the list of deals
    val dailyDealsList = remember { mutableStateListOf<DailyDeal>() }
    // Loading state while fetching data from Firestore
    var isLoading by remember { mutableStateOf(true) }

    // Load daily deals on first composition
    LaunchedEffect(Unit) {
        try {
            val fetchedDeals = dailyDealsRepo.getDailyDeals()
            dailyDealsList.clear()
            dailyDealsList.addAll(fetchedDeals)
        } catch (e: Exception) {
            // You can handle errors here if necessary
        } finally {
            // Small delay to better visualize the loading state, if needed.
            delay(300)
            isLoading = false
        }
    }

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

        if (isLoading) {
            // Show loading indicator
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (dailyDealsList.isEmpty()) {
            // Show empty state if no deals found
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No deals found")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dailyDealsList) { deal ->
                    DailyDealCard(deal)
                }
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
            containerColor = MaterialTheme.colorScheme.secondary
        )) {
            Text(text = "Distance")
        }
        Button(onClick = { /* TODO: Handle filter */ }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )) {
            Text(text = "Price")
        }
        Button(onClick = { /* TODO: Handle filter */ }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )) {
            Text(text = "Sort")
        }
    }
}

@Composable
fun DailyDealCard(deal: DailyDeal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Square cards
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Show the first image if available.
            if (deal.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = deal.images.first()),
                    contentDescription = "Deal Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deal.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Optionally display address or part of the deals details.
            Text(
                text = deal.address,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // For simplicity, display deals for the first day with available info.
            val firstAvailableDay = deal.deals.entries.firstOrNull { it.value.description.isNotEmpty() }
            if (firstAvailableDay != null) {
                Text(
                    text = "${firstAvailableDay.key}: ${firstAvailableDay.value.description.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "No deals available",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
