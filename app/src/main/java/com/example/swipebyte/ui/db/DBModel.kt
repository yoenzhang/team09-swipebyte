package com.example.swipebyte.ui.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


data class Restaurant(
    val name: String,
    val cuisine: String,
    val rating: String,
    val distance: String,
    val imageUrl: String
)


class DBModel {
    companion object {
        private val restaurantList = listOf(
            Restaurant("Lazeez Shawarma", "Middle Eastern - $$$$", "2.6 ★ (1,000+)", "1.5 km away",
                "https://d1ralsognjng37.cloudfront.net/83bb5d98-43b6-4b86-bb9e-4c4c89c11a33.jpeg"
            ),
            Restaurant("Subway", "Fast Food - $$", "3.8 ★ (2,000+)", "800m away",
                "https://www.subway.com/ns/images/hero/Sandwich_Buffet.jpg"
            ),
            Restaurant("McDonald's", "Burgers - $$", "4.1 ★ (5,000+)", "500m away",
                "https://www.mcdonalds.com/content/dam/sites/usa/nfl/publication/1PUB_106_McD_Top20WebsiteRefresh_Photos_QuarterPounderCheese.jpg"
            ),
            Restaurant("Pizza Hut", "Italian - $$$", "4.0 ★ (3,500+)", "2.0 km away",
                "https://www.pizzahut.com/assets/w/tile/th-menu-icon.jpg"
            ),
            Restaurant("temp test", "test - $$$", "4.0 ★ (3,500+)", "2.0 km away",
                "https://www.pizzahut.com/assets/w/tile/th-menu-icon.jpg"
            )
        )

        suspend fun insertData() {
            val db = FirebaseFirestore.getInstance()
            val restaurantCollection = db.collection("restaurants")

            try {
                // Fetch all existing users
                val existingRestaurants = restaurantCollection.get().await()
                    .documents.mapNotNull { it.getString("name") }.toSet()

                val newRestaurants = restaurantList.filter { it.name !in existingRestaurants }

                // Insert new users
                newRestaurants.forEach { restaurant ->
                    restaurantCollection.add(restaurant).await()
                    println("Inserted: ${restaurant.name}")
                }

                println("All restaurants processed!")
            } catch (e: Exception) {
                println("Error: $e")
            }
        }

        suspend fun fetchRestaurants(): List<Restaurant> {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("restaurants")
            val restaurantList = mutableListOf<Restaurant>()

            return try {
                // Fetch all documents from the "restaurants" collection
                val querySnapshot = collectionRef.get().await()

                for (document in querySnapshot) {
                    val name = document.getString("name") ?: "Unknown"
                    val cuisine = document.getString("cuisine") ?: "Unknown"
                    val rating = document.getString("rating") ?: "Unknown"
                    val distance = document.getString("distance") ?: "Unknown"
                    val imageUrl = document.getString("imageUrl") ?: "Unknown"

                    restaurantList.add(Restaurant(name, cuisine, rating, distance, imageUrl))
                }

                restaurantList // Return the populated list
            } catch (e: Exception) {
                println("Error fetching restaurants: $e")
                emptyList() // Return an empty list if fetching fails
            }
        }
    }
}