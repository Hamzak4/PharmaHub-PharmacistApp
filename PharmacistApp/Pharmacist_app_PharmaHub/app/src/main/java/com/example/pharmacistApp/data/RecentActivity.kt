package com.example.pharmacistApp.data

import java.util.Date

data class RecentActivity(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Date,
    val type: PharmaActivityType = PharmaActivityType.OTHER,
    val isNew: Boolean = false,
    val relatedItemId: String? = null,
    val pharmacistId: String = "",
    val prescriptionImageUrl: String? = null,
    val productNames: List<String> = emptyList(),
    val status: String? = null
)
