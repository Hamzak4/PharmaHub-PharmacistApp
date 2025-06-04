package com.example.pharmacistApp.viewmodel

import android.util.Log
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
class PharmacistsListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _pharmacists = MutableLiveData<List<Pharmacist>>()
    val pharmacists: LiveData<List<Pharmacist>> = _pharmacists

    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    private val _currentFilter = MutableLiveData<Int>(0)
    val currentFilter: LiveData<Int> = _currentFilter

    sealed class OperationStatus {
        object Loading : OperationStatus()
        data class Success(val message: String) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    fun loadPharmacists() {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                val query = when (_currentFilter.value) {
                    0 -> firestore.collection("pharmacists") // All
                    1 -> firestore.collection("pharmacists")
                        .whereEqualTo("status", true)
                        .whereEqualTo("isSuspended", false) // Active only (not suspended)
                    2 -> firestore.collection("pharmacists")
                        .whereEqualTo("status", false)
                        .whereEqualTo("isSuspended", false) // Inactive only (not suspended)
                    3 -> firestore.collection("pharmacists")
                        .whereEqualTo("isSuspended", true) // Suspended only
                    else -> firestore.collection("pharmacists")
                }

                val snapshot = query.get().await()
                val pharmacistsList = snapshot.toObjects(Pharmacist::class.java)
                _pharmacists.value = pharmacistsList
                _operationStatus.value = OperationStatus.Success("Data loaded successfully")
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Failed to load data: ${e.message}")
                Log.e("PharmacistsVM", "Load error", e)
            }
        }
    }

    fun loadApprovedPharmacists() {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // Only include active and not suspended pharmacists
                val query = firestore.collection("pharmacists")
                    .whereEqualTo("status", true)
                    .whereEqualTo("isSuspended", false)

                val snapshot = query.get().await()
                val pharmacistsList = snapshot.toObjects(Pharmacist::class.java)
                _pharmacists.value = pharmacistsList
                _operationStatus.value = OperationStatus.Success("Approved pharmacists loaded")
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Failed to load data: ${e.message}")
                Log.e("PharmacistsVM", "Load error", e)
            }
        }
    }

    fun applyFilter(filterIndex: Int) {
        _currentFilter.value = filterIndex
        loadPharmacists()
    }

    fun activatePharmacist(pharmacistId: String) {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .update(mapOf(
                        "status" to true,
                        "lastUpdated" to Date()
                    )).await()

                _operationStatus.value = OperationStatus.Success("Pharmacist activated")
                loadPharmacists() // Refresh the list
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Activation failed: ${e.message}")
            }
        }
    }

    fun deactivatePharmacist(pharmacistId: String) {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .update(mapOf(
                        "status" to false,
                        "lastUpdated" to Date()
                    )).await()

                _operationStatus.value = OperationStatus.Success("Pharmacist deactivated")
                loadPharmacists() // Refresh the list
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Deactivation failed: ${e.message}")
            }
        }
    }

    fun deletePharmacist(pharmacistId: String) {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // First, get the pharmacist to retrieve their user ID
                val pharmacistDoc = firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .get()
                    .await()

                val pharmacist = pharmacistDoc.toObject(Pharmacist::class.java)
                val uid = pharmacist?.uid

                // Delete from Firestore
                firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .delete()
                    .await()

                // If we have the user's UID, request admin deletion through a Cloud Function
                if (!uid.isNullOrEmpty()) {
                    firestore.collection("deletion_requests")
                        .add(mapOf(
                            "uid" to uid,
                            "requestedAt" to Date(),
                            "processed" to false
                        )).await()
                }

                _operationStatus.value = OperationStatus.Success("Pharmacist deleted permanently")
                loadPharmacists() // Refresh the list
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Deletion failed: ${e.message}")
            }
        }
    }

    // UPDATED: When suspending a pharmacist, we set isSuspended=true but maintain their status value
    // This allows us to track whether they were active or inactive before suspension
    fun suspendPharmacist(pharmacistId: String, days: Int) {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // Set canLogin to false to prevent login while suspended
                firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .update(mapOf(
                        "isSuspended" to true,
                        "canLogin" to false,
                        "suspensionEnd" to null, // Set to null or calculate actual end date if needed
                        "lastUpdated" to Date()
                    )).await()

                _operationStatus.value = OperationStatus.Success("Pharmacist suspended for $days days")
                loadPharmacists() // Refresh the list
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Suspension failed: ${e.message}")
            }
        }
    }

    // UPDATED: When removing suspension, we restore their previous status
    // but let it be processed through the applications queue first
    fun removeSuspension(pharmacistId: String) {
        _operationStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // When removing suspension, we keep the status as is but set isSuspended to false
                // This allows admins to review and officially reinstate them
                firestore.collection("pharmacists")
                    .document(pharmacistId)
                    .update(mapOf(
                        "isSuspended" to false,
                        "suspendedfalse" to true, // Mark as previously suspended
                        "suspensionEnd" to null,
                        "lastUpdated" to Date()
                    )).await()

                _operationStatus.value = OperationStatus.Success("Suspension removed, pending approval")
                loadPharmacists() // Refresh the list
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error("Failed to remove suspension: ${e.message}")
            }
        }
    }

    fun exportPharmacistData(): LiveData<OperationStatus> {
        val result = MutableLiveData<OperationStatus>()
        result.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // Get all pharmacists data
                val snapshot = firestore.collection("pharmacists").get().await()
                val pharmacists = snapshot.toObjects(Pharmacist::class.java)

                // Create CSV string
                val csvBuilder = StringBuilder()
                csvBuilder.append("ID,Name,Pharmacy,License,Email,Phone,Address,Status,Orders Completed,Orders Pending,Orders Ongoing\n")

                for (pharmacist in pharmacists) {
                    val status = when {
                        pharmacist.isSuspended == true -> "Suspended"
                        pharmacist.status == true -> "Active"
                        else -> "Inactive"
                    }

                    csvBuilder.append("${pharmacist.pharmacistId},")
                    csvBuilder.append("\"${pharmacist.firstName ?: ""} ${pharmacist.lastName ?: ""}\",")
                    csvBuilder.append("\"${pharmacist.pharmacyName ?: ""}\",")
                    csvBuilder.append("${pharmacist.licenseNumber ?: ""},")
                    csvBuilder.append("${pharmacist.email ?: ""},")
                    csvBuilder.append("${pharmacist.phoneNumber ?: ""},")
                    csvBuilder.append("\"${pharmacist.address ?: ""}, ${pharmacist.city ?: ""}, ${pharmacist.state ?: ""} ${pharmacist.zipCode ?: ""}\",")
                    csvBuilder.append("$status,")
                    csvBuilder.append("${pharmacist.ordersCompleted ?: 0},")
                    csvBuilder.append("${pharmacist.ordersPending ?: 0},")
                    csvBuilder.append("${pharmacist.ordersOngoing ?: 0}\n")
                }

                result.value = OperationStatus.Success("Data exported successfully")
            } catch (e: Exception) {
                result.value = OperationStatus.Error("Export failed: ${e.message}")
            }
        }

        return result
    }
}