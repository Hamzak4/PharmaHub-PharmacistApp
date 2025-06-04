package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class OrderItem(
    val productName: String = "",
    val productId: String = "",
    val quantity: Int = 1,
    val images: List<String>,
    val unitPrice: Double = 0.0,
    val selectedStrength: String? = null,
    val selectedDosageForm: String? = null,
    var totalPrice: Double = 0.0,
    val pharmacistId: String = "",
    val pharmacyAddress: String = "", // Changed to nullable
    val pharmacyName: String? = null,      // Changed to nullable
    val status: String = OrderStatus.ORDERED.name,
    val createdAt: Date = Date(),
    val prescriptionImageUrl: String = ""


) : Parcelable {
    // Calculate total price if not provided
    fun calculateTotalPrice(): Double {
        return unitPrice * quantity
    }
}