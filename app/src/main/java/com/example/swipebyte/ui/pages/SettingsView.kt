package com.example.swipebyte.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swipebyte.ui.navigation.Screen
import com.example.swipebyte.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(navController: NavController, authViewModel: AuthViewModel) {
    val currentUser = authViewModel.getCurrentUser()
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }

    // Password fields
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Error states
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Setup scrolling
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Sign-out button in top right
                    IconButton(
                        onClick = {
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Food Preferences Card
            PreferencesSettingsCard(navController)

            // Location Settings Card
            LocationSettingsCard(navController)

            // Display name section
            DisplayNameSection(
                displayName = displayName,
                onDisplayNameChange = { displayName = it; displayNameError = null },
                displayNameError = displayNameError,
                onUpdateClick = {
                    if (displayName.isBlank()) {
                        displayNameError = "Display name cannot be empty"
                        return@DisplayNameSection
                    }

                    authViewModel.updateDisplayName(displayName) { success, message ->
                        if (success) {
                            successMessage = "Display name updated successfully"
                            showSuccessMessage = true
                        } else {
                            displayNameError = message ?: "Failed to update display name"
                        }
                    }
                }
            )

            // Password section
            PasswordSection(
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                onCurrentPasswordChange = { currentPassword = it; currentPasswordError = null },
                onNewPasswordChange = { newPassword = it; newPasswordError = null },
                onConfirmPasswordChange = { confirmPassword = it; confirmPasswordError = null },
                currentPasswordError = currentPasswordError,
                newPasswordError = newPasswordError,
                confirmPasswordError = confirmPasswordError,
                onUpdateClick = {
                    when {
                        currentPassword.isEmpty() -> {
                            currentPasswordError = "Current password is required"
                        }
                        newPassword.length < 6 -> {
                            newPasswordError = "Password must be at least 6 characters"
                        }
                        newPassword != confirmPassword -> {
                            confirmPasswordError = "Passwords do not match"
                        }
                        else -> {
                            // Use the new re-authentication method
                            authViewModel.reauthenticateAndUpdatePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword
                            ) { success, message ->
                                if (success) {
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                    successMessage = "Password updated successfully"
                                    showSuccessMessage = true
                                } else {
                                    // Check if the error is related to authentication
                                    if (message?.contains(
                                            "authentication",
                                            ignoreCase = true
                                        ) == true ||
                                        message?.contains(
                                            "credential",
                                            ignoreCase = true
                                        ) == true
                                    ) {
                                        currentPasswordError = message
                                    } else {
                                        newPasswordError = message ?: "Failed to update password"
                                    }
                                }
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

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
}

@Composable
fun PreferencesSettingsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate(Screen.Preferences.route) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Food Preferences",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Food Preferences",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Set your cuisine types, dietary restrictions, and price range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go to Preferences Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LocationSettingsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate(Screen.Location.route) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location Settings",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Set your location and search radius for restaurants",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go to Location Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DisplayNameSection(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    displayNameError: String?,
    onUpdateClick: () -> Unit
) {
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
                onValueChange = onDisplayNameChange,
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = displayNameError != null,
                supportingText = {
                    if (displayNameError != null) {
                        Text(
                            displayNameError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Name")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpdateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Display Name")
            }
        }
    }
}

@Composable
fun PasswordSection(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    currentPasswordError: String?,
    newPasswordError: String?,
    confirmPasswordError: String?,
    onUpdateClick: () -> Unit
) {
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

            // Current password field
            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                isError = currentPasswordError != null,
                supportingText = {
                    if (currentPasswordError != null) {
                        Text(
                            currentPasswordError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Current Password"
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // New password field
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                isError = newPasswordError != null,
                supportingText = {
                    if (newPasswordError != null) {
                        Text(
                            newPasswordError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "New Password")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPasswordError != null,
                supportingText = {
                    if (confirmPasswordError != null) {
                        Text(
                            confirmPasswordError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Confirm Password"
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpdateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Password")
            }
        }
    }
}
