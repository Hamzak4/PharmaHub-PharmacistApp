package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import kotlinx.parcelize.RawValue
import java.util.*

@Parcelize
data class Prescription(
    var id: String = "",
    val approved: Boolean = false,
    val rejected: Boolean = false,
    val pending: Boolean = true,
    val status: String = "pending",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val timestamp: Long = 0,
    val prescriptionImageUrl: String = "",
    val userId: String = "",
    val pharmacistId: String? = null,
    val orderId: String? = null,
    val note: String? = null,
    val used: Boolean = false,
    val usedInOrder: String? = null,
    val productIds: List<String> = emptyList(),
    val products: @RawValue List<Map<String, Any>>? = null,
    var productDetails: List<ProductDetail>? = null
) : Parcelable {

    @Parcelize
    data class ProductDetail(
        val name: String = "",
        val genericName: String = "",
        val strength: String = "",
        val dosageForm: String = "",
        val productId: String = ""
    ) : Parcelable

    val formattedDate: String
        get() {
            val date = Date(if (createdAt != 0L) createdAt else timestamp)
            return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }

    val formattedDateTime: String
        get() {
            val date = Date(if (createdAt != 0L) createdAt else timestamp)
            return SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)
        }
}
