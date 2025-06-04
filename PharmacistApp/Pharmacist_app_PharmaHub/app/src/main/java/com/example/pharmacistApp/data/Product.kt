package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Product(
    val id: String = "",
    val name: String = "",
    val genericName: String = "",
    val category: String = "",
    val price: Float = 0f,
    val offerPercentage: Float? = null,
    val imageUrl: String = "",
    val productId: String = "",
    val threshold: Int = 5,
    val description: String? = null,
    val dosageForm: String = "",
    val strengths: List<String> = emptyList(),
    val manufacturer: String? = null,
    val images: List<String> = emptyList(),
    val requiresPrescription: Boolean = false,
    val activeIngredients: List<String> = emptyList(),
    val sideEffects: String? = null,
    val storageInstructions: String? = null,
    val pharmacistId: String = "",
    val pharmacyName: String = "", // Added pharmacy name
    val pharmacyAddress: String = "", // Changed from coordinates to address
    val quantity: Int = 1,

    val availableQuantity: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    var inStock: Boolean = false

) : Parcelable {
    // Ensure inStock is always consistent with quantity
    init {
        inStock = quantity > 0
    }
}