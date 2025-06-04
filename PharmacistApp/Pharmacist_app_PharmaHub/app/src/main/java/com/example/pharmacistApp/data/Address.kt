package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    val addressTitle: String = "",
    val fullName: String = "",
    val street: String = "",
    val phone: String = "",
    val city: String = "",
    val state: String = ""
) : Parcelable {
    // Helper function to format full address
    fun getFormattedAddress(): String {
        return "$street, $city, $state"
    }
}