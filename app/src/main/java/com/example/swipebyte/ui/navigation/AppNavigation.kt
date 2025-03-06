package com.example.swipebyte.ui.navigation  // ✅ Use lowercase for "swipebyte"

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.swipebyte.R
import com.example.swipebyte.ui.pages.CommunityFavouritesView
import com.example.swipebyte.ui.pages.DealsOfTheDayView
import com.example.swipebyte.ui.pages.FriendRequestView
import com.example.swipebyte.ui.pages.HomeView
import com.example.swipebyte.ui.pages.LoginScreen
import com.example.swipebyte.ui.pages.ProfileView
import com.example.swipebyte.ui.pages.SignUpScreen
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel
import com.example.swipebyte.ui.viewmodel.FriendViewModel


sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int? = null) {
    object Login : Screen("login", "Login")
    object SignUp: Screen("signup", "SignUp")
    object Home : Screen("home", "Home", R.drawable.foodicon)
    object DealsOfTheDay : Screen("dealsOfTheDay", "Deals", R.drawable.heartcheck)
    object CommunityFavourites : Screen("communityFavourites", "Community", R.drawable.star)
    object Profile : Screen("profile", "Profile", R.drawable.profile)
    object FriendRequests : Screen("friendRequests", "Friend Requests")
}
@Composable
fun AppNavigation(authViewModel: AuthViewModel, friendViewModel: FriendViewModel, userId: LiveData<String?>) {
    val navController = rememberNavController()

    // Observe login status from ViewModel
    val isLoggedIn by authViewModel.isLoggedIn.observeAsState(false)

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
                composable(Screen.Profile.route) { ProfileView(navController, authViewModel) }
                composable(Screen.FriendRequests.route) {
                    val userId by userId.observeAsState()
                    // Handle null userId case and pass non-null value to the FriendRequestView
                    userId?.let { id ->
                        // Safe to pass 'id' as it's guaranteed non-null
                        FriendRequestView(navController, id)
                    } ?: run {
                        // Handle the case where userId is null (you can show a loading screen or error)
                        // For example, you can log an error or show a fallback UI
                        Log.e("AppNavigation", "User ID is null in FriendRequests screen")
                    }
                }
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

    NavigationBar(
        containerColor = Color(0xFFF7F7F7), // Custom light gray background color
        modifier = Modifier
            .shadow(elevation = 5.dp, spotColor = Color.Black),
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = screen.title,
                            modifier = Modifier.size(35.dp),  // Adjust the size here
                        )
                    }
                },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.LightGray,
                    indicatorColor = Color.Transparent // Remove the background box
                )
            )
        }
    }
}



