package com.example.pharmacistApp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UsersListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUsers()
    }

    // In UsersListViewModel.kt
    private fun loadUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("UsersList", "Fetching from 'user' collection")
                val snapshot = firestore.collection("user").get().await() // Ensure this matches your collection name

                val userList = snapshot.documents.mapNotNull { document ->
                    try {
                        User(
                            id = document.id,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            email = document.getString("email") ?: "",
                            imagePath = document.getString("imagePath") ?: "",
                            isActive = document.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        Log.e("UsersList", "Error parsing user ${document.id}", e)
                        null
                    }
                }

                Log.d("UsersList", "Fetched ${userList.size} users")
                _users.value = userList
            } catch (e: Exception) {
                Log.e("UsersList", "Failed to load users", e)
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshUsers() {
        loadUsers()
    }
}