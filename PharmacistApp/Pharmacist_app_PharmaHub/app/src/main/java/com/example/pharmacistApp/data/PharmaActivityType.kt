package com.example.pharmacistApp.data

enum class PharmaActivityType {
    NEW_USER,
    NEW_ORDER,
    PHARMACY_APPLICATION,
    COMPLAINT,
    LOW_STOCK,
    NEW_PRESCRIPTION,
    OTHER;

    companion object {
        fun fromString(type: String?): PharmaActivityType {
            return when (type?.uppercase()) {
                "NEW_USER" -> NEW_USER
                "NEW_ORDER" -> NEW_ORDER
                "PHARMACY_APPLICATION" -> PHARMACY_APPLICATION
                "COMPLAINT" -> COMPLAINT
                "LOW_STOCK" -> LOW_STOCK
                "NEW_PRESCRIPTION" -> NEW_PRESCRIPTION
                else -> OTHER
            }
        }
    }
}