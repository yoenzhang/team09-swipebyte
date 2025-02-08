package com.example.swipebyte.ui.navigation  // ✅ Use lowercase for "swipebyte"

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.swipebyte.ui.pages.CommunityFavouritesView
import com.example.swipebyte.ui.pages.DealsOfTheDayView
import com.example.swipebyte.ui.pages.HomeView
import com.example.swipebyte.ui.pages.LoginScreen
import com.example.swipebyte.ui.pages.ProfileView
import com.example.swipebyte.ui.pages.SignUpScreen
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel


sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object SignUp: Screen("signup", "SignUp")
    object Home : Screen("home", "Home")
    object DealsOfTheDay : Screen("dealsOfTheDay", "Deals")
    object CommunityFavourites : Screen("communityFavourites", "Community")
    object Profile : Screen("profile", "Profile")
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    // Observe login status from ViewModel
    val isLoggedIn by authViewModel.isLoggedIn.observeAsState(false)
    Log.d("test", isLoggedIn.toString())

    SwipeByteTheme {
        Scaffold(
            bottomBar = {
                if (isLoggedIn) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeView(navController) }
                composable(Screen.DealsOfTheDay.route) { DealsOfTheDayView(navController) }
                composable(Screen.CommunityFavourites.route) { CommunityFavouritesView(navController) }
                composable(Screen.Profile.route) { ProfileView(navController) }
                composable(Screen.Login.route) {
                    LoginScreen(authViewModel, navController)
                }
                composable(Screen.SignUp.route) {
                    SignUpScreen(authViewModel, navController)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.DealsOfTheDay,
        Screen.CommunityFavourites,
        Screen.Profile
    )

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