package com.example.pharmacistApp.data

import androidx.annotation.DrawableRes
import com.example.pharmacistApp.R

enum class OrderStatus(
    val displayName: String,
    @DrawableRes val iconRes: Int
) {
    READY_FOR_DELIVERY("Ready for Delivery", R.drawable.bg_status_ready),
    ORDERED("Ordered", R.drawable.bg_status_waiting),
    PROCESSING("Processing", R.drawable.bg_status_active),
    SHIPPED("Shipped", R.drawable.bg_status_prepared),
    DELIVERED("Delivered", R.drawable.bg_status_delivered),
    CANCELLED("Cancelled", R.drawable.bg_status_rejected),
    PENDING("Pending", R.drawable.bg_status_pending);

    companion object {
        fun fromString(value: String): OrderStatus {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}