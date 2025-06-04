package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.Pharmacist
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PharmacyApplicationsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _pharmacists = MutableLiveData<List<Pharmacist>>()
    val pharmacists: LiveData<List<Pharmacist>> = _pharmacists

    private val _suspendedPharmacists = MutableLiveData<List<Pharmacist>>()
    val suspendedPharmacists: LiveData<List<Pharmacist>> = _suspendedPharmacists

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // Load both new applications and suspended pharmacists
    // Update loadPharmacistApplications() to handle the field name mismatch
    fun loadPharmacistApplications() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Get all pharmacists to properly filter
                val allPharmacistsSnapshot = firestore.collection("pharmacists")
                    .get()
                    .await()

                val allPharmacists = allPharmacistsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Pharmacist>()?.copy(pharmacistId = doc.id)
                }

                // Filter new applications (status = false AND not suspended)
                val newApplications = allPharmacists.filter { pharmacist ->
                    !pharmacist.status && !pharmacist.isSuspended
                }

                // Filter suspended pharmacists (isSuspended = true)
                val suspendedPharmacists = allPharmacists.filter { pharmacist ->
                    pharmacist.isSuspended
                }

                // Update LiveData
                _pharmacists.value = newApplications
                _suspendedPharmacists.value = suspendedPharmacists
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "Failed to load applications: ${e.message}"
            }
        }
    }

    // Approve a new pharmacist application
    fun approvePharmacist(pharmacistId: String) {
        _isLoading.value = true
        firestore.collection("pharmacists").document(pharmacistId)
            .update(mapOf(
                "status" to true,
                "isSuspended" to false,
                "suspensionEnd" to null,
                "lastUpdated" to Date()
            ))
            .addOnSuccessListener {
                _message.value = "Pharmacist approved successfully"
                loadPharmacistApplications()
            }
            .addOnFailureListener {
                _isLoading.value = false
                _message.value = "Failed to approve pharmacist"
            }
    }

    // Reinstate a suspended pharmacist
    fun reinstateSuspendedPharmacist(pharmacistId: String) {
        _isLoading.value = true
        firestore.collection("pharmacists").document(pharmacistId)
            .update(mapOf(
                "status" to true,
                "isSuspended" to false,
                "suspensionEnd" to null,
                "lastUpdated" to Date()
            ))
            .addOnSuccessListener {
                _message.value = "Pharmacist reinstated successfully"
                loadPharmacistApplications()
            }
            .addOnFailureListener {
                _isLoading.value = false
                _message.value = "Failed to reinstate pharmacist"
            }
    }

    // Reject a pharmacist application
    fun rejectPharmacist(pharmacistId: String) {
        _isLoading.value = true
        firestore.collection("pharmacists").document(pharmacistId)
            .delete()
            .addOnSuccessListener {
                _message.value = "Pharmacist application rejected"
                loadPharmacistApplications()
            }
            .addOnFailureListener {
                _isLoading.value = false
                _message.value = "Failed to reject application"
            }
    }

    fun clearMessage() {
        _message.value = null
    }
}