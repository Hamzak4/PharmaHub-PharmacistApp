package com.example.pharmacistApp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.Complaint
import com.example.pharmacistApp.data.ComplaintResponse
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ComplaintsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _complaintsState = MutableStateFlow<Resource<List<Complaint>>>(Resource.Loading())
    val complaintsState: StateFlow<Resource<List<Complaint>>> = _complaintsState

    private var complaintsListener: ListenerRegistration? = null
    private val TAG = "ComplaintsViewModel"

    init {
        // Initialize with empty list
        _complaintsState.value = Resource.Success(emptyList())
    }

    fun fetchPendingComplaints() {
        _complaintsState.value = Resource.Loading()
        complaintsListener?.remove()

        try {
            // Check if we need to use a different approach if index isn't ready
            val isIndexCreated = true // Replace with shared preference check if needed

            complaintsListener = if (isIndexCreated) {
                // Original query with sorting - requires composite index
                firestore.collection("complaints")
                    .whereEqualTo("status", "pending")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshots, error ->
                        handleComplaintsSnapshot(snapshots, error, "pending")
                    }
            } else {
                // Fallback query without sorting - doesn't require composite index
                // Only use this temporarily while index is being created
                firestore.collection("complaints")
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshots, error ->
                        handleComplaintsSnapshot(snapshots, error, "pending")
                    }
            }
        } catch (e: Exception) {
            handleError(e, "Error setting up pending complaints listener")
        }
    }

    fun fetchComplaintById(complaintId: String) {
        viewModelScope.launch {
            _complaintsState.value = Resource.Loading()
            try {
                // Get the complaint document
                val documentSnapshot = firestore.collection("complaints")
                    .document(complaintId)
                    .get()
                    .await()

                if (documentSnapshot.exists()) {
                    try {
                        // Convert raw data to complaint object manually to avoid deserialization issues
                        val data = documentSnapshot.data
                        if (data != null) {
                            val complaint = Complaint(
                                id = documentSnapshot.id,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                userEmail = data["userEmail"] as? String ?: "",
                                text = data["text"] as? String ?: "",
                                type = data["type"] as? String ?: "",
                                status = data["status"] as? String ?: "pending",
                                timestamp = data["timestamp"] ?: null,
                                respondedAt = data["respondedAt"] ?: null,
                                lastResponseBy = data["lastResponseBy"] as? String
                            )

                            // Fetch responses for this complaint
                            val responses = fetchResponsesForComplaint(complaintId)

                            // Add responses to the complaint
                            val complaintWithResponses = complaint.copyWithResponses(responses)

                            _complaintsState.value = Resource.Success(listOf(complaintWithResponses))
                            Log.d(TAG, "Fetched complaint $complaintId with ${responses.size} responses")
                        } else {
                            _complaintsState.value = Resource.Error("Complaint data is null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing complaint: ${e.message}", e)
                        _complaintsState.value = Resource.Error("Failed to parse complaint: ${e.message}")
                    }
                } else {
                    _complaintsState.value = Resource.Error("Complaint not found")
                }
            } catch (e: Exception) {
                handleError(e, "Failed to fetch complaint details for $complaintId")
            }
        }
    }

    fun fetchComplaintsByUserId(userId: String) {
        _complaintsState.value = Resource.Loading()
        complaintsListener?.remove()

        try {
            complaintsListener = firestore.collection("complaints")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    handleComplaintsSnapshot(snapshots, error, "user_$userId")
                }
        } catch (e: Exception) {
            handleError(e, "Error setting up user complaints listener")
        }
    }

    // Update this method in your ComplaintsViewModel.kt

    private fun handleComplaintsSnapshot(
        snapshots: QuerySnapshot?,
        error: Exception?,
        queryType: String
    ) {
        if (error != null) {
            handleFirestoreError(error, queryType)
            return
        }

        viewModelScope.launch {
            try {
                val complaints = withContext(Dispatchers.IO) {
                    snapshots?.documents?.mapNotNull { doc ->
                        try {
                            // Get complaint object - will auto-populate the @DocumentId field
                            val complaint = doc.toObject(Complaint::class.java)
                            if (complaint == null) {
                                Log.w(TAG, "Null complaint object for document ${doc.id}")
                                return@mapNotNull null
                            }

                            // Fetch responses for this complaint
                            val responses = fetchResponsesForComplaint(doc.id)

                            // Add responses to the complaint
                            complaint.copyWithResponses(responses)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing complaint ${doc.id}: ${e.message}", e)
                            // Create fallback minimal object
                            try {
                                val data = doc.data
                                if (data != null) {
                                    Complaint(
                                        id = doc.id,
                                        status = data["status"] as? String ?: "unknown",
                                        text = data["text"] as? String ?: "No text available",
                                        timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                                        type = data["type"] as? String ?: "unknown",
                                        userEmail = data["userEmail"] as? String ?: "",
                                        userId = data["userId"] as? String ?: "",
                                        userName = data["userName"] as? String ?: "Unknown User"
                                    )
                                } else null
                            } catch (e2: Exception) {
                                Log.e(TAG, "Failed fallback parsing for ${doc.id}: ${e2.message}", e2)
                                null
                            }
                        }
                    } ?: emptyList()
                }

                _complaintsState.value = Resource.Success(complaints)
                Log.d(TAG, "Fetched ${complaints.size} complaints ($queryType)")
            } catch (e: Exception) {
                handleError(e, "Error processing complaints snapshot")
            }
        }
    }
    private fun handleFirestoreError(error: Exception, queryType: String) {
        Log.e(TAG, "Error fetching $queryType complaints", error)

        val errorMessage = when {
            error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION &&
                    error.message?.contains("requires an index") == true -> {
                "This query requires a Firestore index. Please wait while the database is being optimized or contact support."
            }
            error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                "You don't have permission to access these complaints."
            }
            else -> error.message ?: "Failed to load complaints"
        }

        _complaintsState.value = Resource.Error(errorMessage)
    }

    private fun handleError(e: Exception, context: String) {
        Log.e(TAG, context, e)
        _complaintsState.value = Resource.Error(
            e.localizedMessage ?: "An unexpected error occurred"
        )
    }

    private suspend fun fetchResponsesForComplaint(complaintId: String): List<ComplaintResponse> {
        return try {
            val snapshot = firestore.collection("complaints")
                .document(complaintId)
                .collection("responses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    // Manual conversion to avoid deserialization issues
                    val data = doc.data
                    if (data != null) {
                        ComplaintResponse(
                            id = doc.id,
                            adminId = data["adminId"] as? String ?: "",
                            adminEmail = data["adminEmail"] as? String,
                            response = data["response"] as? String ?: "",
                            timestamp = data["timestamp"]
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response ${doc.id}: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching responses for complaint $complaintId: ${e.message}", e)
            emptyList()
        }
    }

    fun addComplaintResponse(complaintId: String, response: String, callback: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Get current admin details
                val adminId = auth.currentUser?.uid ?: "unknown"
                val adminEmail = auth.currentUser?.email ?: "admin@pharmacy.com"

                // Add response to subcollection
                val responseRef = firestore.collection("complaints")
                    .document(complaintId)
                    .collection("responses")
                    .document()

                val responseData = hashMapOf(
                    "id" to responseRef.id,
                    "adminId" to adminId,
                    "adminEmail" to adminEmail,
                    "response" to response,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                responseRef.set(responseData).await()

                // Update complaint status
                firestore.collection("complaints")
                    .document(complaintId)
                    .update(
                        mapOf(
                            "status" to "responded",
                            "respondedAt" to FieldValue.serverTimestamp(),
                            "lastResponseBy" to adminEmail
                        )
                    ).await()

                // Refresh the complaints to show updated status
                refreshComplaintById(complaintId)

                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding response to complaint $complaintId", e)
                withContext(Dispatchers.Main) {
                    _complaintsState.value = Resource.Error(
                        "Failed to send response: ${e.message ?: "Please try again"}"
                    )
                    callback(false)
                }
            }
        }
    }

    private fun refreshComplaintById(complaintId: String) {
        fetchComplaintById(complaintId)
    }

    // Add a new method for refreshing pending complaints
    fun refreshPendingComplaints() {
        fetchPendingComplaints()
    }

    override fun onCleared() {
        complaintsListener?.remove()
        super.onCleared()
    }
}