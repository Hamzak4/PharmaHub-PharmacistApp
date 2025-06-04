package com.example.pharmacistApp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.Address
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.data.OrderItem
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.data.RecentActivity
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PharmacistDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _pharmacistData = MutableStateFlow<Resource<Pharmacist>>(Resource.Loading())
    val pharmacistData: StateFlow<Resource<Pharmacist>> = _pharmacistData

    private val _dashboardStats = MutableStateFlow<Resource<DashboardStats>>(Resource.Loading())
    val dashboardStats: StateFlow<Resource<DashboardStats>> = _dashboardStats

    private val _recentActivities = MutableStateFlow<Resource<List<RecentActivity>>>(Resource.Loading())
    val recentActivities: StateFlow<Resource<List<RecentActivity>>> = _recentActivities



    private var ordersListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var prescriptionsListener: ListenerRegistration? = null
    private var activitiesListener: ListenerRegistration? = null


    fun updatePrescriptionCount(count: Int) {
        val currentStats = (_dashboardStats.value as? Resource.Success)?.data ?: DashboardStats()
        _dashboardStats.value = Resource.Success(
            currentStats.copy(pendingPrescriptions = count)
        )
    }

    data class DashboardStats(
        val pendingOrders: Int = 0,
        val lowStockItems: Int = 0,
        val pendingPrescriptions: Int = 0
    )


    fun loadPharmacistData(pharmacistId: String) {
        viewModelScope.launch {
            _pharmacistData.value = Resource.Loading()
            try {
                val document = firestore.collection("pharmacists").document(pharmacistId).get().await()
                if (document.exists()) {
                    _pharmacistData.value = Resource.Success(document.toPharmacist())
                } else {
                    _pharmacistData.value = Resource.Error("Pharmacist not found")
                }
            } catch (e: Exception) {
                _pharmacistData.value = Resource.Error(e.localizedMessage ?: "Failed to load pharmacist data")
            }
        }
    }

    fun loadDashboardStats(pharmacistId: String) {
        setupStatsListeners(pharmacistId)
    }

    fun loadRecentActivities(pharmacistId: String) {
        setupActivitiesListener(pharmacistId)
    }

    private fun setupActivitiesListener(pharmacistId: String) {
        activitiesListener?.remove()

        // Listen to both activities collection and prescriptions collection
        val activitiesQuery = firestore.collection("activities")
            .whereEqualTo("pharmacistId", pharmacistId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)

        val prescriptionsQuery = firestore.collection("prescriptions")
            // Remove pharmacistId filter since it's null
            //.whereEqualTo("pharmacistId", pharmacistId)
            .whereEqualTo("status", "pending") // Changed from "pending_review" to "pending"
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)

        activitiesListener = activitiesQuery.addSnapshotListener { activitiesSnapshot, activitiesError ->
            prescriptionsQuery.get().addOnSuccessListener { prescriptionsSnapshot ->
                val combinedActivities = mutableListOf<RecentActivity>()

                // Add regular activities
                activitiesSnapshot?.documents?.mapNotNullTo(combinedActivities) { doc ->
                    try {
                        doc.toActivity()
                    } catch (e: Exception) {
                        null
                    }
                }

                // Add prescription activities
                // In setupActivitiesListener()
                prescriptionsSnapshot.documents.mapNotNullTo(combinedActivities) { doc ->
                    try {
                        if (doc.getString("status") == "pending") {
                            val productIds = doc.get("productIds") as? List<String> ?: emptyList()
                            val productNames = doc.get("products")?.let { products ->
                                (products as? List<Map<String, Any>>)?.mapNotNull {
                                    it["name"] as? String
                                } ?: emptyList()
                            } ?: emptyList()

                            RecentActivity(
                                id = doc.id,
                                title = "New Prescription",
                                description = "Requires your review",
                                timestamp = doc.getDate("timestamp") ?: Date(),
                                type = PharmaActivityType.NEW_PRESCRIPTION,
                                isNew = true,
                                relatedItemId = doc.id,
                                pharmacistId = pharmacistId,
                                prescriptionImageUrl = doc.getString("prescriptionImageUrl"),
                                productNames = productNames,
                                status = doc.getString("status")
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                // Sort combined activities by timestamp
                combinedActivities.sortByDescending { it.timestamp }

                // Limit to 10 most recent
                _recentActivities.value = Resource.Success(combinedActivities.take(10))
            }.addOnFailureListener { e ->
                _recentActivities.value = Resource.Error("Failed to load prescriptions: ${e.message}")
            }
        }
    }
    private fun setupStatsListeners(pharmacistId: String) {
        // Clear existing listeners
        ordersListener?.remove()
        productsListener?.remove()
        prescriptionsListener?.remove()

        // Orders listener - count pending orders
        ordersListener = firestore.collection("orders")
            .whereEqualTo("pharmacistId", pharmacistId)
            .whereEqualTo("orderStatus", "Ordered")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("DashboardVM", "Orders error", it)
                    return@addSnapshotListener
                }
                val count = snapshot?.documents?.size ?: 0
                updateStats(pendingOrders = count)
            }
        // In setupStatsListeners()
        prescriptionsListener = firestore.collection("prescriptions")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("DashboardVM", "Prescriptions error", it)
                    return@addSnapshotListener
                }
                val count = snapshot?.documents?.size ?: 0
                Log.d("DashboardVM", "Found $count pending prescriptions")
                updateStats(pendingPrescriptions = count)
            }

        // Products listener - count low stock items
        productsListener = firestore.collection("Products")
            .whereEqualTo("pharmacistId", pharmacistId)
            .whereLessThanOrEqualTo("quantity", 5) // or availableQuantity if you use that
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("DashboardVM", "Products error", it)
                    return@addSnapshotListener
                }
                val count = snapshot?.documents?.size ?: 0
                updateStats(lowStockItems = count)
            }

        // Prescriptions listener - count pending prescriptions
        prescriptionsListener = firestore.collection("prescriptions")
            // Remove pharmacistId filter
            //.whereEqualTo("pharmacistId", pharmacistId)
            .whereEqualTo("status", "pending") // Changed from "pending_review" to "pending"
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("DashboardVM", "Prescriptions error", it)
                    return@addSnapshotListener
                }
                val count = snapshot?.documents?.size ?: 0
                updateStats(pendingPrescriptions = count)
            }
    }

    private fun updateStats(
        pendingOrders: Int? = null,
        lowStockItems: Int? = null,
        pendingPrescriptions: Int? = null
    ) {
        try {
            val current = _dashboardStats.value.data ?: DashboardStats()
            _dashboardStats.value = Resource.Success(
                current.copy(
                    pendingOrders = pendingOrders ?: current.pendingOrders,
                    lowStockItems = lowStockItems ?: current.lowStockItems,
                    pendingPrescriptions = pendingPrescriptions ?: current.pendingPrescriptions
                )
            )
        } catch (e: Exception) {
            _dashboardStats.value = Resource.Error("Error updating stats: ${e.message}")
        }
    }



    private fun DocumentSnapshot.toPharmacist(): Pharmacist {
        return Pharmacist(
            pharmacistId = id,
            uid = getString("uid") ?: "",
            firstName = getString("firstName") ?: "",
            lastName = getString("lastName") ?: "",
            email = getString("email") ?: "",
            phoneNumber = getString("phoneNumber") ?: "",
            profileImageUrl = getString("profileImageUrl"),
            pharmacyName = getString("pharmacyName") ?: "",
            licenseNumber = getString("licenseNumber") ?: "",
            address = getString("address") ?: ""
        )
    }

    private fun DocumentSnapshot.toActivity(): RecentActivity {
        return RecentActivity(
            id = id,
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            timestamp = getDate("timestamp") ?: Date(),
            type = PharmaActivityType.fromString(getString("type")),
            isNew = getBoolean("isNew") ?: false,
            relatedItemId = getString("relatedItemId") ?: "",
            pharmacistId = getString("pharmacistId") ?: ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
        productsListener?.remove()
        prescriptionsListener?.remove()
        activitiesListener?.remove()
    }
}