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

    private val friendRepo = FriendRepo()  // Initialize FriendRepo directly in ViewModel

    private val _pendingRequests = MutableLiveData<List<FriendRequest>>()
    val pendingRequests: LiveData<List<FriendRequest>> = _pendingRequests

    private val _operationResult = MutableLiveData<Result<Boolean>>()
    val operationResult: LiveData<Result<Boolean>> = _operationResult

    fun sendFriendRequest(senderId: String, receiverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepo.sendFriendRequest(senderId, receiverId) { success ->
                    _operationResult.postValue(Result.success(success))
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
                    _operationResult.postValue(Result.success(success))
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
                    _operationResult.postValue(Result.success(success))
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

    fun clearOperationResult() {
        _operationResult.value = Result.success(false)
    }
}
