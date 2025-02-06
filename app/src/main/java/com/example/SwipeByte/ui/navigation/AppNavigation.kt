package com.example.SwipeByte.navigation  // ✅ Use lowercase for "swipebyte"

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.SwipeByte.ui.pages.*
import com.example.SwipeByte.ui.theme.SwipeByteTheme
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.SwipeByte.ui.viewmodel.AuthViewModel


sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object Home : Screen("home", "Home")
    object DealsOfTheDay : Screen("dealsOfTheDay", "Deals")
    object Notifications : Screen("notifications", "Notifications")
    object Profile : Screen("profile", "Profile")
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    // Observe login status from ViewModel
    val isLoggedIn by authViewModel.isLoggedIn.observeAsState(false)

    SwipeByteTheme {
        Scaffold(
            bottomBar = { BottomNavigationBar(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeView(navController) }
                composable(Screen.DealsOfTheDay.route) { DealsOfTheDayView(navController) }
                composable(Screen.Notifications.route) { NotificationsView(navController) }
                composable(Screen.Profile.route) { ProfileView(navController) }
                composable(Screen.Login.route) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.DealsOfTheDay, Screen.Notifications, Screen.Profile)

    val currentBackStackEntry by navController.currentBackStackEntryAsState() // ✅ FIXED
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) },
                icon = {} // Icons can be added here
            )
        }
    }
}
