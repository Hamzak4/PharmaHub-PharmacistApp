package com.example.pharmacistApp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class Order(
    val id: String = "",
    val orderId: String = System.currentTimeMillis().toString(), // Can handle both String and Long
    val customerPhone: String = "",
    val totalPrice: Double = 0.0,
    val orderStatus: String = OrderStatus.PENDING.name,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()),
    val customerName: String = "",
    val phone: String = "",
    val prescriptionData: List<Prescription> = emptyList(),
    val deliveryAddress: String = "",
    val paymentMethod: String = "",
    val notes: String? = null,
    val pharmacyName: String? = null,
    val deliveryInstructions: String = "",
    val prescriptions: List<Prescription> = emptyList(),
    val prescriptionIds: List<String> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val items: List<OrderItem> = emptyList(),
    val updatedAt: Date = Date(),
    val address: Address = Address(),
    val products: List<OrderItem> = emptyList(),
    val prescriptionUrl: String? = null,
    val prescriptionApproved: Boolean? = null,
    val rejectionReason: String? = null,
    val createdAt: Date = Date(),  // Changed from Long to Date
    val userId: String = "",
    val pharmacistId: String? = null,
    val pharmacyLocation: String? = null,
    var prescriptionId: String? = null,
    val courierInfo: CourierInfo? = null,
) : Parcelable {

    companion object {
        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }



    val displayStatus: OrderStatus
        get() = try {
            OrderStatus.valueOf(orderStatus)
        } catch (e: IllegalArgumentException) {
            OrderStatus.PENDING
        }

    val mainPharmacist: PharmacistInfo?
        get() = pharmacistId?.let {
            PharmacistInfo(
                id = it,
                name = pharmacyName,
                location = pharmacyLocation
            )
        }

    val allPharmacists: List<PharmacistInfo>
        get() {
            val pharmacists = mutableSetOf<PharmacistInfo>()
            products.forEach { product ->
                product.pharmacistId?.let { id ->
                    pharmacists.add(
                        PharmacistInfo(
                            id = id,
                            name = product.pharmacyName,
                            location = product.pharmacyAddress
                        )
                    )
                }
            }
            return pharmacists.toList()
        }

    fun productsByPharmacist(pharmacistId: String): List<OrderItem> {
        return products.filter { it.pharmacistId == pharmacistId }
    }

    fun hasPrescription(): Boolean = !prescriptionUrl.isNullOrEmpty()

    val canBeApproved: Boolean
        get() = displayStatus == OrderStatus.PENDING && hasPrescription()

    val canBeRejected: Boolean
        get() = displayStatus == OrderStatus.PENDING

    val canBeProcessed: Boolean
        get() = displayStatus == OrderStatus.PENDING

    @Parcelize
    data class PharmacistInfo(
        val id: String,
        val name: String?,
        val location: String?
    ) : Parcelable
}