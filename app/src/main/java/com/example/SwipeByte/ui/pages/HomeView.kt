package com.example.swipebyte.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swipebyte.navigation.Screen
import com.example.SwipeByte.ui.theme.Typography  // ✅ Fixed
import com.example.SwipeByte.ui.theme.Purple80    // ✅ Fixed

@Composable
fun HomeView(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏠 Home View",
            style = Typography.headlineLarge // ✅ Uses typography from `theme/Type.kt`
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(Screen.Dashboard.route) },
            colors = ButtonDefaults.buttonColors(containerColor = Purple80) // ✅ Uses colors from `theme/Color.kt`
        ) {
            Text("Go to Dashboard")
        }
    }
}
