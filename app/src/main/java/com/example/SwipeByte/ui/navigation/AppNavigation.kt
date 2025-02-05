package com.example.swipebyte.navigation  // ✅ Use lowercase for "swipebyte"

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.swipebyte.ui.pages.*
import com.example.SwipeByte.ui.theme.SwipeByteTheme
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Notifications : Screen("notifications", "Notifications")
    object Profile : Screen("profile", "Profile")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SwipeByteTheme { // ✅ Ensure the theme is wrapped properly
        Scaffold(
            bottomBar = { BottomNavigationBar(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeView(navController) }
                composable(Screen.Dashboard.route) { DashboardView(navController) }
                composable(Screen.Notifications.route) { NotificationsView(navController) }
                composable(Screen.Profile.route) { ProfileView(navController) }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Dashboard, Screen.Notifications, Screen.Profile)

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
