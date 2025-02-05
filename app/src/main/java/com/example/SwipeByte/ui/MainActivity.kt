package com.example.SwipeByte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.SwipeByte.navigation.AppNavigation
import com.example.SwipeByte.ui.theme.SwipeByteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwipeByteTheme {
                AppNavigation()
            }
        }
    }
}