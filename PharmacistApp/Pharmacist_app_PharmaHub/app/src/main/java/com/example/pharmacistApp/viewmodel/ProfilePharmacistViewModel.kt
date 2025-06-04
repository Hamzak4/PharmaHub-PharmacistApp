package com.example.pharmacistApp.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.pharmacistApp.cloudinary.CloudinaryHelper
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class ProfilePharmacistViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryHelper: CloudinaryHelper
) : ViewModel() {

    private val pharmacistsRef = firestore.collection("pharmacists")

    private val _pharmacistData = MutableLiveData<Resource<Pharmacist>>()
    val pharmacistData: LiveData<Resource<Pharmacist>> = _pharmacistData

    private val _uploadStatus = MutableLiveData<Resource<String>>()
    val uploadStatus: LiveData<Resource<String>> = _uploadStatus

    fun getPharmacistProfile(pharmacistId: String) {
        viewModelScope.launch {
            _pharmacistData.postValue(Resource.Loading())
            try {
                val document = pharmacistsRef.document(pharmacistId).get().await()
                if (!document.exists()) {
                    throw Exception("Pharmacist not found")
                }
                _pharmacistData.postValue(Resource.Success(document.toPharmacist()))
            } catch (e: Exception) {
                _pharmacistData.postValue(Resource.Error("Failed to load profile: ${e.message}"))
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri, pharmacistId: String) {
        viewModelScope.launch {
            _uploadStatus.postValue(Resource.Loading())
            try {
                val imageUrl = withContext(Dispatchers.IO) {
                    suspendCoroutine<String> { continuation ->
                        cloudinaryHelper.uploadImage(
                            uri = imageUri,
                            onSuccess = continuation::resume,
                            onError = { error -> continuation.resumeWithException(Exception(error)) }
                        )
                    }
                }

                pharmacistsRef.document(pharmacistId)
                    .update("profileImageUrl", imageUrl)
                    .await()

                _uploadStatus.postValue(Resource.Success(imageUrl))
            } catch (e: Exception) {
                _uploadStatus.postValue(Resource.Error("Upload failed: ${e.message}"))
            }
        }
    }

    private fun DocumentSnapshot.toPharmacist() = Pharmacist(
        pharmacistId = id,
        firstName = getString("firstName").orEmpty(),
        lastName = getString("lastName").orEmpty(),
        fullName = getString("fullName").orEmpty(),
        email = getString("email").orEmpty(),
        phoneNumber = getString("phoneNumber").orEmpty(),
        licenseNumber = getString("licenseNumber").orEmpty(),
        pharmacyName = getString("pharmacyName").orEmpty(),
        address = getString("address").orEmpty(),
        city = getString("city").orEmpty(),
        state = getString("state").orEmpty(),
        zipCode = getString("zipCode").orEmpty(),
        fullAddress = getString("fullAddress").orEmpty(),
        profileImageUrl = getString("profileImageUrl"),
        status = getBoolean("status") ?: false,
        isSuspended = getBoolean("isSuspended") ?: false,
        suspensionEnd = getTimestamp("suspensionEnd")?.toDate(),
        lastUpdated = getTimestamp("lastUpdated")?.toDate()
    )
}