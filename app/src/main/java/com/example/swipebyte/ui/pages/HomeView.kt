package com.example.swipebyte.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.ui.db.DBModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    LaunchedEffect(Unit) {
        val fetchedRestaurants = DBModel.fetchRestaurants()
        restaurantList.addAll(fetchedRestaurants)
    }

    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            count = restaurantList.size,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            RestaurantCard(
                restaurant = restaurantList[page],
                onSwiped = { direction ->
                    coroutineScope.launch {
                        println("Swiped $direction: hello world")
                        val nextPage = (pagerState.currentPage + 1) % restaurantList.size
                        pagerState.animateScrollToPage(nextPage)
                    }
                }
            )
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onSwiped: (String) -> Unit) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = 300f

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetValue = when {
                                offsetX.value > swipeThreshold -> {
                                    onSwiped("Right")
                                    1000f
                                }
                                offsetX.value < -swipeThreshold -> {
                                    onSwiped("Left")
                                    -1000f
                                }
                                else -> 0f
                            }
                            offsetX.animateTo(
                                targetValue = targetValue,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount)
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = restaurant.imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    .height((LocalConfiguration.current.screenHeightDp * 0.15f).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = restaurant.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = restaurant.cuisine,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = restaurant.rating,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = restaurant.distance,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
