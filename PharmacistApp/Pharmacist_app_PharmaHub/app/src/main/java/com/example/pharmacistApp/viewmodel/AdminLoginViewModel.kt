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
import javax.inject.Inject

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun loginAdmin(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Authenticate the user
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("Authentication failed")

                // Log UID for debugging
                println("DEBUG: Authenticated UID: $uid")

                // Check Firestore for admin document
                val adminDoc = firestore.collection("admins").document(uid).get().await()

                // Log Firestore results for debugging
                println("DEBUG: Document exists: ${adminDoc.exists()}")
                println("DEBUG: Document data: ${adminDoc.data}")

                if (adminDoc.exists()) {
                    _loginState.value = LoginState.Success
                } else {
                    firebaseAuth.signOut()
                    _loginState.value = LoginState.Error("Admin privileges not found")
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                _loginState.value = LoginState.Error("Admin account not found")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _loginState.value = LoginState.Error("Invalid email or password")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Login failed: ${e.localizedMessage}")
            }
        }
    }
}
