package com.example.pharmacistApp.data

data class OrderReport(
    val totalOrders: Int,
    val completedOrders: Int,
    val pendingOrders: Int,
    val totalRevenue: Double,
    val completionRate: Float,
    val recentOrders: List<OrderReportItem>
)