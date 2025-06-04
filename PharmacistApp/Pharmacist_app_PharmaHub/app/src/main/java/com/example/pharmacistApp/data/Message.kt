package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Message(
    var messageId: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val isAdmin: Boolean = false,
    val isRead: Boolean = false
) : Parcelable {
    // No-arg constructor for Firestore
    constructor() : this("", "", "", "", 0L, false, false)
}