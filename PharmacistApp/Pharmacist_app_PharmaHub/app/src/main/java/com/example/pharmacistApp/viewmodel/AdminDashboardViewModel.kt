package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.ActivityType
import com.example.pharmacistApp.data.DashboardData
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.RecentActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _dashboardData = MutableLiveData<DashboardData>()
    val dashboardData: LiveData<DashboardData> = _dashboardData

    private val _loadingState = MutableLiveData<LoadingState>()
    val loadingState: LiveData<LoadingState> = _loadingState

    fun loadDashboardData() {
        _loadingState.value = LoadingState.Loading

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    // Get dashboard stats
                    val statsDoc = firestore.collection("dashboard_stats")
                        .document("admin_dashboard")
                        .get()
                        .await()

                    // Get recent activities separately with ordering
                    val recentActivitiesQuery = firestore.collection("recent_activities")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(10)
                        .get()
                        .await()

                    val activities = recentActivitiesQuery.documents.mapNotNull { doc ->
                        try {
                            RecentActivity(
                                id = doc.id,
                                type = PharmaActivityType.valueOf(doc.getString("type") ?: "NEW_USER"),
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date(),
                                isNew = doc.getBoolean("isNew") ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    DashboardData(
                        totalUsers = statsDoc.getLong("totalUsers")?.toInt() ?: 0,
                        totalOrders = statsDoc.getLong("totalOrders")?.toInt() ?: 0,
                        activePharmacies = statsDoc.getLong("activePharmacies")?.toInt() ?: 0,
                        pendingComplaints = statsDoc.getLong("pendingComplaints")?.toInt() ?: 0,
                        adminName = statsDoc.getString("adminName"),
                        recentActivities = activities
                    )
                }

                _dashboardData.value = data
                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load dashboard data")
            }
        }
    }

    fun refreshDashboard() {
        loadDashboardData()
    }

    fun markActivityAsRead(activityId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("recent_activities")
                    .document(activityId)
                    .update("isNew", false)
                    .await()

                // Update local data to reflect the change
                _dashboardData.value?.let { currentData ->
                    val updatedActivities = currentData.recentActivities.map { activity ->
                        if (activity.id == activityId) activity.copy(isNew = false) else activity
                    }
                    _dashboardData.value = currentData.copy(recentActivities = updatedActivities)
                }
            } catch (e: Exception) {
                // Handle silently or notify if needed
            }
        }
    }
}

sealed class LoadingState {
    object Loading : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}