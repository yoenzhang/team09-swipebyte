package com.example.swipebyte.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swipebyte.ui.db.models.FriendRequest
import com.example.swipebyte.ui.db.repository.FriendRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {

    private val friendRepo = FriendRepo()  // Initialize FriendRepo

    private val _pendingRequests = MutableLiveData<List<Pair<FriendRequest, String>>>()
    val pendingRequests: LiveData<List<Pair<FriendRequest, String>>> get() = _pendingRequests

    private val _friendsList = MutableLiveData<List<Pair<String, String>>>()
    val friendsList: LiveData<List<Pair<String, String>>> get() = _friendsList

    private val _operationResult = MutableLiveData<Result<String>>() // Changed from Result<Boolean> to Result<String>
    val operationResult: LiveData<Result<String>> = _operationResult

    fun sendFriendRequest(senderId: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.getUserIdByEmail(email) { receiverId ->
                    if (receiverId != null) {
                        if (senderId == receiverId) {
                            _operationResult.postValue(Result.failure(Exception("You cannot send a friend request to yourself.")))
                        } else {
                            // Updated to receive a string message instead of boolean
                            friendRepo.sendFriendRequest(senderId, receiverId) { message ->
                                // Only post success if no previous failure occurred
                                if (_operationResult.value?.isFailure != true) {
                                    _operationResult.postValue(Result.success(message))
                                    loadPendingRequests(senderId)
                                }
                            }
                        }
                    } else {
                        _operationResult.postValue(Result.failure(Exception("User not found with the email $email")))
                    }
                }
            } catch (e: Exception) {
                _operationResult.postValue(Result.failure(e))
            }
        }
    }

    fun acceptFriendRequest(requestId: String, senderId: String, receiverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.acceptFriendRequest(requestId, senderId, receiverId) { success ->
                    val message = if (success) "Friend request accepted successfully." else "Failed to accept friend request."
                    _operationResult.postValue(Result.success(message))
                    loadPendingRequests(receiverId)
                }
            } catch (e: Exception) {
                _operationResult.postValue(Result.failure(e))
            }
        }
    }

    fun declineFriendRequest(requestId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.declineFriendRequest(requestId) { success ->
                    val message = if (success) "Friend request declined successfully." else "Failed to decline friend request."
                    _operationResult.postValue(Result.success(message))
                    loadPendingRequests(userId)
                }
            } catch (e: Exception) {
                _operationResult.postValue(Result.failure(e))
            }
        }
    }

    fun loadPendingRequests(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.loadPendingRequests(userId) { requests ->
                    _pendingRequests.postValue(requests)
                }
            } catch (e: Exception) {
                _pendingRequests.postValue(emptyList())
            }
        }
    }

    fun loadFriendsList(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.loadFriendsList(userId) { friends ->
                    _friendsList.postValue(friends)
                }
            } catch (e: Exception) {
                Log.e("FriendViewModel", "Error loading friends list", e)
                _friendsList.postValue(emptyList())
                _operationResult.postValue(Result.failure(Exception("Failed to load friends list")))
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = Result.success("")
    }
}

