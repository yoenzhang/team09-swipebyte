package com.example.swipebyte.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.swipebyte.ui.db.DBModel
import com.example.swipebyte.ui.navigation.AppNavigation
import com.example.swipebyte.ui.theme.SwipeByteTheme
import com.example.swipebyte.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            DBModel.insertData()
        }
        super.onCreate(savedInstanceState)
        setContent {
            SwipeByteTheme {
                AppNavigation(authViewModel)
            }
        }
    }
}