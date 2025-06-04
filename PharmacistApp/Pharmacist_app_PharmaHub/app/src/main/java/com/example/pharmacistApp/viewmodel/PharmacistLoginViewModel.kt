package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PharmacistLoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
        object NotApproved : LoginState()
        object Suspended : LoginState()
        object AlreadyAuthenticated : LoginState()
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            firebaseAuth.currentUser?.uid?.let { userId ->
                _loginState.value = LoginState.Loading
                try {
                    val pharmacistDoc = firestore.collection("pharmacists")
                        .document(userId)
                        .get()
                        .await()

                    if (!pharmacistDoc.exists()) {
                        firebaseAuth.signOut()
                        return@launch
                    }

                    val isApproved = pharmacistDoc.getBoolean("status") ?: false
                    val isSuspended = pharmacistDoc.getBoolean("isSuspended") ?: false
                    val suspensionEnd = pharmacistDoc.getTimestamp("suspensionEnd")?.toDate()

                    when {
                        isSuspended && suspensionEnd?.after(Date()) == true -> {
                            firebaseAuth.signOut()
                            _loginState.value = LoginState.Suspended
                        }
                        !isApproved -> {
                            firebaseAuth.signOut()
                            _loginState.value = LoginState.NotApproved
                        }
                        else -> {
                            _loginState.value = LoginState.AlreadyAuthenticated
                        }
                    }
                } catch (e: Exception) {
                    firebaseAuth.signOut()
                    _loginState.value = LoginState.Error("Session validation failed")
                }
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Authentication failed")

                val pharmacistDoc = firestore.collection("pharmacists")
                    .document(userId)
                    .get()
                    .await()

                if (!pharmacistDoc.exists()) {
                    firebaseAuth.signOut()
                    throw Exception("Pharmacist profile not found")
                }

                val isApproved = pharmacistDoc.getBoolean("status") ?: false
                val isSuspended = pharmacistDoc.getBoolean("isSuspended") ?: false
                val suspensionEnd = pharmacistDoc.getTimestamp("suspensionEnd")?.toDate()

                when {
                    isSuspended && suspensionEnd?.after(Date()) == true -> {
                        firebaseAuth.signOut()
                        _loginState.value = LoginState.Suspended
                    }
                    !isApproved -> {
                        firebaseAuth.signOut()
                        _loginState.value = LoginState.NotApproved
                    }
                    else -> {
                        _loginState.value = LoginState.Success
                    }
                }
            } catch (e: Exception) {
                firebaseAuth.signOut()
                _loginState.value = LoginState.Error(
                    when (e) {
                        is FirebaseAuthInvalidUserException -> "Account not found"
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                        else -> "Login failed: ${e.message}"
                    }
                )
            }
        }
    }
}