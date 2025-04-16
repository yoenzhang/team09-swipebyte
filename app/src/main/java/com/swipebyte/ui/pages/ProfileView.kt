package com.swipebyte.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swipebyte.ui.navigation.Screen
import com.swipebyte.ui.viewmodel.AuthViewModel
@Composable

fun ProfileView(navController: NavController, authViewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "👤 Profile", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate(Screen.Home.route) }) {
            Text("Go to Home")
        }
        Button(onClick = {
            authViewModel.logout()
        }) {
            Text("Log Out")
        }

        // Send Friend Request Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate(Screen.FriendRequests.route) }
        ) {
            Text("Go to Friend Requests")
        }
    }
}
