package com.example.pharmacistApp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ComplaintResponse(
    val id: String = "",
    val adminId: String = "",
    val adminEmail: String? = null,
    val response: String = "",
    val timestamp: Any? = null
)