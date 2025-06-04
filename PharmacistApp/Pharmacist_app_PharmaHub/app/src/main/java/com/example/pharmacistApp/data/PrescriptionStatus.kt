package com.example.pharmacistApp.data

enum class PrescriptionStatus {
    PENDING,          // Waiting for pharmacist review
    APPROVED,         // Verified and accepted
    REJECTED,         // Declined with reason
    EXPIRED           // No longer valid
}