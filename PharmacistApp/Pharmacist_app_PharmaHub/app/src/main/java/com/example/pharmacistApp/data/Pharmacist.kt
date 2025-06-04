package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Pharmacist(
    // Primary identifiers
    val pharmacistId: String = "",
    val uid: String = "",

    // Personal information
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",  // Pre-computed full name
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String? = null,

    // Pharmacy information
    val pharmacyName: String = "",
    val licenseNumber: String = "",

    // Address components
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val fullAddress: String = "",      // Pre-computed combined address string
    val pharmacyLocation: String = "", // City/state combination

    // Status information
    val status: Boolean = false,
    val isSuspended: Boolean = false,
    val suspensionEnd: Date? = null,
    val lastUpdated: Date? = null,

    // Order statistics
    val ordersCompleted: Int = 0,
    val ordersPending: Int = 0,
    val ordersOngoing: Int = 0
) : Parcelable {

    val canLogin: Boolean
        get() = status && !isSuspended

    // Helper to get complete location string
    val completeLocation: String
        get() = if (city.isNotEmpty() && state.isNotEmpty()) {
            "$city, $state $zipCode"
        } else {
            fullAddress
        }

    // Helper to get display name
    val displayName: String
        get() = fullName.ifEmpty { "$firstName $lastName" }

    // Helper to get total orders
    val totalOrders: Int
        get() = ordersCompleted + ordersPending + ordersOngoing
}