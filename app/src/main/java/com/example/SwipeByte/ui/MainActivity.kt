package com.example.SwipeByte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.SwipeByte.navigation.AppNavigation
import com.example.SwipeByte.ui.theme.SwipeByteTheme
import com.example.SwipeByte.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwipeByteTheme {
                AppNavigation(authViewModel)
            }
        }
    }
}