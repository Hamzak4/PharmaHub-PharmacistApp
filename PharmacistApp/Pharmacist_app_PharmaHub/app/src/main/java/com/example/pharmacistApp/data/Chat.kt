package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Chat(
    var chatId: String = "",
    val adminId: String = "",
    val chatType: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerProfileImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val orderId: String = "",
    val pharmacistId: String = "",
    val status: String = "active",
    val unreadCount: Int = 0
) : Parcelable {
    companion object {
        const val TYPE_APP_SUPPORT = "app_support"
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESOLVED = "resolved"
    }
}