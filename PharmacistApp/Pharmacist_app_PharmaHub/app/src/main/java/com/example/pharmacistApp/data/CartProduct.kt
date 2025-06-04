package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartProduct(
    val product: Product,
    val quantity: Int,
    val selectedStrength: String? = null,  // Changed from selectedColor
    val selectedDosageForm: String? = null,
    val pharmacistId: String = "",  // Added
    val pharmacyLocation: String = "" ,
    val totalPrice: Double = 0.0,
    val pharmacyName: String? = null// Added// Changed from selectedSize
) : Parcelable {
    constructor() : this(Product(), 1, null, null)

    // Helper function to display key product info
    fun displayInfo(): String {
        return buildString {
            append(product.name)
            selectedStrength?.let { append(" ($it)") }
            selectedDosageForm?.let { append(" - $it") }
        }
    }
}