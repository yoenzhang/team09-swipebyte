package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    val viewModel: FriendViewModel = viewModel() // Correctly instantiate using the factory

    val pendingRequests by viewModel.pendingRequests.observeAsState(emptyList())
    val operationResult by viewModel.operationResult.observeAsState()
    var emailInput by remember { mutableStateOf("") }

    // Handle operation results with side effects

    LaunchedEffect(key1 = userId) {
        viewModel.loadPendingRequests(userId)
        Log.d("FriendRequests", "LaunchedEffect - Loading pending requests for user: $userId")
    }

    LaunchedEffect(operationResult) {
        operationResult?.let { result ->
            if (result.isSuccess) {
                // Optionally show a success toast or snackbar
                Log.d("FriendRequest", "Operation successful")
                // Clear any previous errors
                viewModel.clearOperationResult()
            } else {
                // Optionally show an error toast or snackbar
                Log.e("FriendRequest", "Operation failed", result.exceptionOrNull())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button
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
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add Friends",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input for adding a friend by email
        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Enter Email to Add Friend") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to send a friend request
        Button(
            onClick = {
                if (emailInput.isNotEmpty()) {

                    Log.d("FriendRequest", "Attempting to send friend request to $emailInput")
                    viewModel.sendFriendRequest(
                        senderId = userId,
                        email = emailInput
                    )

                    emailInput = ""
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Friend Request")
        }

        // Scrollable list of friend requests
        LazyColumn {
            items(pendingRequests) { (request, name) ->
                FriendRequestItem(
                    request = request,
                    name = name,
                    onAccept = {
                        viewModel.acceptFriendRequest(
                            requestId = request.requestId,
                            senderId = request.senderId,
                            receiverId = userId
                        )
                    },
                    onDecline = {
                        viewModel.declineFriendRequest(
                            requestId = request.requestId,
                            userId = userId
                        )
                    }
                )
            }
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
