package com.example.pharmacistApp.data

data class DashboardData(
    val totalUsers: Int = 0,
    val totalOrders: Int = 0,
    val activePharmacies: Int = 0,
    val pendingComplaints: Int = 0,
    val adminName: String? = null,
    val recentActivities: List<RecentActivity> = emptyList()
)


