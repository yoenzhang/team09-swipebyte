package com.example.swipebyte.ui.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.swipebyte.R
import com.example.swipebyte.ui.pages.*
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel
import com.example.swipebyte.ui.viewmodel.FriendViewModel

sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int? = null) {
    object Login : Screen("login", "Login")
    object SignUp : Screen("signup", "SignUp")
    object Home : Screen("home", "Home", R.drawable.foodicon)
    object DealsOfTheDay : Screen("dealsOfTheDay", "Deals", R.drawable.heartcheck)
    object CommunityFavourites : Screen("communityFavourites", "Community", R.drawable.star)
    object ProfileSettings : Screen("profileSettings", "Profile", R.drawable.profile)
    object FriendRequests : Screen("friendRequests", "Friend Requests")
    object MyLikes : Screen("myLikes", "My Likes", R.drawable.profile)
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    friendViewModel: FriendViewModel,
    userId: LiveData<String?>  // The user’s Firestore userId
) {
    val navController = rememberNavController()
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
                composable(Screen.Home.route) {
                    HomeView(navController)
                }
                composable(Screen.DealsOfTheDay.route) {
                    DealsOfTheDayView(navController)
                }
                composable(Screen.CommunityFavourites.route) {
                    CommunityFavouritesView(navController)
                }
                // MyLikes route - pass userId to MyLikesView
                composable(Screen.MyLikes.route) {
                    val currentUserId by userId.observeAsState()
                    currentUserId?.let { userIdVal ->
                        MyLikesView(
                            navController = navController,
                            userId = userIdVal
                        )
                    } ?: run {
                        Log.e("AppNavigation", "User ID is null in MyLikes screen")
                    }
                }
                composable(Screen.ProfileSettings.route) {
                    ProfileView(navController, authViewModel)
                }
                composable(Screen.FriendRequests.route) {
                    val userIdVal by userId.observeAsState()
                    userIdVal?.let { id ->
                        FriendRequestView(navController, id)
                    } ?: run {
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
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.DealsOfTheDay,
        Screen.CommunityFavourites,
        Screen.MyLikes
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color(0xFFF7F7F7),
        modifier = Modifier.shadow(elevation = 5.dp, spotColor = Color.Black)
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = screen.title,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Save and restore state to keep previous destinations in memory
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.LightGray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
