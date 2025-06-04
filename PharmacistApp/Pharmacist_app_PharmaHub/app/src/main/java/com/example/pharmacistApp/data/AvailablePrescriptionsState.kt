package com.example.pharmacistApp.data

sealed class AvailablePrescriptionsState {
    object Loading : AvailablePrescriptionsState()
    data class Success(val prescriptions: List<Prescription>) : AvailablePrescriptionsState()
    data class Error(val message: String) : AvailablePrescriptionsState()
}