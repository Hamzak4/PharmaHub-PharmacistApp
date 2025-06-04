package com.example.pharmacistApp.data

enum class SortOrder {
    ORDER_ID_ASC,          // Sort by order ID (ascending)
    ORDER_ID_DESC,         // Sort by order ID (descending)
    NEWEST_FIRST,          // Sort by creation date (newest first)
    OLDEST_FIRST,          // Sort by creation date (oldest first)
    PRICE_HIGH_TO_LOW,     // Sort by total price (high to low)
    PRICE_LOW_TO_HIGH,     // Sort by total price (low to high)
    PHARMACY_NAME,     // Sort by pharmacy name (A-Z)

}