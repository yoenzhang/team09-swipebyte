package com.example.swipebyte.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swipebyte.ui.navigation.Screen
import com.example.swipebyte.ui.viewmodel.AuthViewModel


@Composable
fun SettingsView(navController: NavController, authViewModel: AuthViewModel) {
    val currentUser = authViewModel.getCurrentUser()
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var displayNameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display name section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Update Display Name",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        displayNameError = null
                    },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = displayNameError != null,
                    supportingText = {
                        if (displayNameError != null) {
                            Text(displayNameError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Name")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (displayName.isBlank()) {
                            displayNameError = "Display name cannot be empty"
                            return@Button
                        }

                        authViewModel.updateDisplayName(displayName) { success, message ->
                            if (success) {
                                successMessage = "Display name updated successfully"
                                showSuccessMessage = true
                            } else {
                                displayNameError = message ?: "Failed to update display name"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Display Name")
                }
            }
        }

        // Password section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(passwordError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmPasswordError = null
                    },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPasswordError != null,
                    supportingText = {
                        if (confirmPasswordError != null) {
                            Text(confirmPasswordError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        when {
                            password.length < 6 -> {
                                passwordError = "Password must be at least 6 characters"
                            }
                            password != confirmPassword -> {
                                confirmPasswordError = "Passwords do not match"
                            }
                            else -> {
                                authViewModel.updatePassword(password) { success, message ->
                                    if (success) {
                                        password = ""
                                        confirmPassword = ""
                                        successMessage = "Password updated successfully"
                                        showSuccessMessage = true
                                    } else {
                                        passwordError = message ?: "Failed to update password"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Password")
                }
            }
        }

        // Log out option
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Log Out")
        }

        // Success message dialog
        if (showSuccessMessage) {
            AlertDialog(
                onDismissRequest = { showSuccessMessage = false },
                title = { Text("Success") },
                text = { Text(successMessage) },
                confirmButton = {
                    Button(onClick = { showSuccessMessage = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
