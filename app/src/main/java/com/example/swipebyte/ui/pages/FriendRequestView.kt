package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swipebyte.ui.db.models.FriendRequest
import com.example.swipebyte.ui.db.repository.FriendRepo
import com.example.swipebyte.ui.viewmodel.FriendViewModel

@Composable
fun FriendRequestView(
    navController: NavController,
    userId: String
) {
    val viewModel: FriendViewModel = viewModel()
    val pendingRequests by viewModel.pendingRequests.observeAsState(emptyList())
    val friendsList by viewModel.friendsList.observeAsState(emptyList())
    val operationResult by viewModel.operationResult.observeAsState()
    var emailInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadPendingRequests(userId)
        viewModel.loadFriendsList(userId)
    }

    LaunchedEffect(operationResult) {
        operationResult?.let { result ->
            val message = result.getOrNull()?.takeIf { it.isNotBlank() }
                ?: result.exceptionOrNull()?.message // Get exception message if it's a failure
                ?: if (result.isSuccess) "Operation successful!" else "Operation failed."

            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearOperationResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Add Friends", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Enter Email to Add Friend") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (emailInput.isNotEmpty()) {
                        viewModel.sendFriendRequest(userId, emailInput)
                        emailInput = ""
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Send Friend Request")
            }

            // Friend Requests Section
            if (pendingRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Friend Requests (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    pendingRequests.forEach { (request, name) ->
                        FriendRequestItem(
                            request = request,
                            name = name,
                            onAccept = { viewModel.acceptFriendRequest(request.requestId, request.senderId, userId) },
                            onDecline = { viewModel.declineFriendRequest(request.requestId, userId) }
                        )
                    }
                }
            }

            // Friends List Section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Friends (${friendsList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (friendsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No friends yet. Send a friend request to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    friendsList.forEach { (friendId, friendName) ->
                        FriendItem(friendId = friendId, friendName = friendName)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(friendId: String, friendName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = friendName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
fun FriendRequestItem(
    request: FriendRequest,
    name: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(15.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge)

        Row {
            // Accept Button
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Accept")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Decline Button
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Decline")
            }
        }
    }
}
