
package com.example.pharmacistApp.data

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Complaint(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val text: String = "",
    val type: String = "",
    val status: String = "pending",
    val timestamp: Any? = null,
    val respondedAt: Any? = null,
    val lastResponseBy: String? = null,
    val responses: List<ComplaintResponse> = emptyList()
) {
    // Helper method to create a copy with responses
    fun copyWithResponses(responses: List<ComplaintResponse>): Complaint {
        return copy(responses = responses)
    }
}