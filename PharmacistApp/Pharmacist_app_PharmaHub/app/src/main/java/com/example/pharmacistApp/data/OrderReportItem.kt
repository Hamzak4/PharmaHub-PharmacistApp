package com.example.pharmacistApp.data

data class OrderReportItem(
    val orderId: String,
    val customerName: String,
    val date: String,
    val status: String,
    val total: Double
)