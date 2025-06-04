package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.Pharmacist
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PharmacistRegistrationViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    sealed class RegistrationState {
        object Loading : RegistrationState()
        object Success : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState

    fun registerPharmacist(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        pharmacyName: String,
        licenseNumber: String,
        phoneNumber: String,
        address: String,
        city: String,
        state: String,
        zipCode: String
    ) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                // 1. Create Firebase user
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("User creation failed")

                // 2. Create full address
                val fullAddress = "$address, $city, $state $zipCode"

                // 3. Create pharmacist object
                val pharmacist = Pharmacist(
                    pharmacistId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    fullName = "$firstName $lastName",
                    email = email,
                    phoneNumber = phoneNumber,
                    licenseNumber = licenseNumber,
                    pharmacyName = pharmacyName,
                    address = address,
                    city = city,
                    state = state,
                    zipCode = zipCode,
                    fullAddress = fullAddress,
                    status = false, // Needs admin approval
                    isSuspended = false,
                    lastUpdated = Date()
                )

                // 4. Save to Firestore
                firestore.collection("pharmacists")
                    .document(userId)
                    .set(pharmacist)
                    .await()

                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                // Clean up on failure
                try {
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
                _registrationState.value = RegistrationState.Error(
                    when (e) {
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                            "Password must be at least 8 characters"
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                            "Email already in use"
                        else -> "Registration failed: ${e.message}"
                    }
                )
            }
        }
    }
}