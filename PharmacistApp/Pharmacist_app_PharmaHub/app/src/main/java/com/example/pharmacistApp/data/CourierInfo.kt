package com.example.pharmacistApp.data



import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourierInfo(
    val name: String,
    val phone: String,
    val photoUrl: String = "",
    val trackingNumber: String? = null,
    val trackingUrl: String = ""
) : Parcelable
