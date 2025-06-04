package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    sealed class ResetState {
        object Idle : ResetState() // <--- Add this
        object Loading : ResetState()
        object Success : ResetState()
        data class Error(val message: String) : ResetState()
    }


    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)

    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                _resetState.value = ResetState.Success
            } catch (e: Exception) {
                _resetState.value = ResetState.Error("Failed to send reset email: ${e.message}")
            }
        }
    }
}