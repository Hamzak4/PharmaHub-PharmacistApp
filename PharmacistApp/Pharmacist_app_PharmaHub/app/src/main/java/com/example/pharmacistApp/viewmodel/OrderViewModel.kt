package com.example.pharmacistApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import android.util.Log
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val ordersCollection = firestore.collection("orders")
    private val pharmacistsCollection = firestore.collection("pharmacists")

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    private val _currentOrder = MutableLiveData<Resource<Order>>()
    val currentOrder: LiveData<Resource<Order>> = _currentOrder

    private val _statusUpdateResult = MutableLiveData<Resource<Boolean>>()
    val statusUpdateResult: LiveData<Resource<Boolean>> = _statusUpdateResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var allOrders = emptyList<Order>()
    private var pharmacistCache = emptyMap<String, Pharmacist>()
    private var currentPharmacistId: String? = null
    private val currentFilter = MutableLiveData<OrderStatus?>()

    init {
        loadInitialOrders()
    }

    fun setPharmacistFilter(pharmacistId: String?) {
        currentPharmacistId = pharmacistId
        loadInitialOrders()
    }

    fun refreshOrders() = loadInitialOrders()

    private fun loadInitialOrders() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                pharmacistCache = fetchPharmacists()
                allOrders = fetchOrdersWithPharmacyDetails()
                applyFiltersAndSorting()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error loading orders: ${e.message}"
                Log.e("OrderViewModel", "Error loading orders", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchOrderById(orderId: String) {
        _currentOrder.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val document = firestore.collection("orders")
                    .document(orderId)
                    .get()
                    .await()

                if (!document.exists()) {
                    _currentOrder.value = Resource.Error("Order not found")
                    return@launch
                }

                val order = parseOrderDocument(document)
                _currentOrder.value = Resource.Success(order)
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error fetching order", e)
                _currentOrder.value = Resource.Error("Failed to load order details: ${e.message}")
            }
        }
    }

    private suspend fun parseOrderDocument(document: DocumentSnapshot): Order {
        val data = document.data ?: throw Exception("Order data is null")

        // Parse prescriptions
        val prescriptions = fetchPrescriptionsForOrder(document)

        // Create a map of product IDs to their prescription images
        val productPrescriptionMap = mutableMapOf<String, String>()
        prescriptions.forEach { prescription ->
            (data["products"] as? List<Map<String, Any>>)?.forEach { product ->
                if ((product["product"] as? Map<String, Any>)?.get("id") == prescription.id) {
                    productPrescriptionMap[prescription.id] = prescription.prescriptionImageUrl
                }
            }
        }

        // Parse courier info if available
        val courierInfo = (data["courierInfo"] as? Map<String, Any>)?.let {
            CourierInfo(
                name = it["name"] as? String ?: "",
                phone = it["phone"] as? String ?: "",
                photoUrl = it["photoUrl"] as? String ?: "",
                trackingNumber = it["trackingNumber"] as? String,
                trackingUrl = it["trackingUrl"] as? String ?: ""
            )
        }

        return Order(
            id = document.id,
            orderId = data["orderId"] as? String ?: document.id,
            userId = data["userId"] as? String ?: "",
            date = data["date"] as? String ?: "",
            totalPrice = data["totalPrice"] as? Double ?: 0.0,
            status = OrderStatus.fromString(data["orderStatus"] as? String ?: ""),
            paymentMethod = data["paymentMethod"] as? String ?: "",
            deliveryInstructions = data["deliveryInstructions"] as? String ?: "",
            address = parseAddress(data["address"] as? Map<String, Any>),
            products = parseProducts(data["products"] as? List<Map<String, Any>>, productPrescriptionMap),
            prescriptions = prescriptions,
            courierInfo = courierInfo,
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        )
    }

    private fun parseProducts(
        productsList: List<Map<String, Any>>?,
        prescriptionMap: Map<String, String> = emptyMap()
    ): List<OrderItem> {
        return productsList?.mapNotNull { productMap ->
            try {
                val productObj = productMap["product"] as? Map<String, Any> ?: return@mapNotNull null

                // Only include products for the current pharmacist if filter is active
                val pharmacistId = productMap["pharmacistId"] as? String ?: ""
                if (currentPharmacistId != null && pharmacistId != currentPharmacistId) {
                    return@mapNotNull null
                }

                val productId = productObj["id"] as? String ?: ""

                // Extract images properly
                val images = when (val imagesData = productObj["images"]) {
                    is String -> listOf(imagesData)
                    is List<*> -> imagesData.filterIsInstance<String>()
                    else -> emptyList()
                }

                OrderItem(
                    productId = productId,
                    productName = productObj["name"] as? String ?: "",
                    quantity = (productMap["quantity"] as? Long)?.toInt() ?: 1,
                    images = images,
                    unitPrice = (productObj["price"] as? Double) ?: 0.0,
                    totalPrice = productMap["totalPrice"] as? Double ?: 0.0,
                    pharmacistId = pharmacistId,
                    pharmacyName = productMap["pharmacyName"] as? String ?: "",
                    pharmacyAddress = productMap["pharmacyAddress"] as? String ?: "",
                    status = productMap["status"] as? String ?: OrderStatus.PENDING.name,
                    selectedStrength = productMap["selectedStrength"] as? String,
                    selectedDosageForm = productMap["selectedDosageForm"] as? String,
                    prescriptionImageUrl = prescriptionMap[productId] ?: ""
                )
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error parsing product", e)
                null
            }
        } ?: emptyList()
    }

    fun assignCourier(orderId: String, courierInfo: CourierInfo) {
        _statusUpdateResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                // Validate input
                if (courierInfo.name.isBlank() || courierInfo.phone.isBlank()) {
                    _statusUpdateResult.value = Resource.Error("Courier name and phone are required")
                    return@launch
                }

                // Update Firestore
                ordersCollection.document(orderId).update(
                    mapOf(
                        "courierInfo" to hashMapOf(
                            "name" to courierInfo.name,
                            "phone" to courierInfo.phone,
                            "trackingNumber" to courierInfo.trackingNumber,
                            "photoUrl" to courierInfo.photoUrl,
                            "trackingUrl" to courierInfo.trackingUrl
                        ),
                        "orderStatus" to OrderStatus.SHIPPED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Update successful
                _statusUpdateResult.value = Resource.Success(true)
                fetchOrderById(orderId) // Refresh the order data
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error assigning courier", e)
                _statusUpdateResult.value = Resource.Error("Failed to assign courier: ${e.message}")
            }
        }
    }

    fun markAsDelivered(orderId: String) {
        _statusUpdateResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                ordersCollection.document(orderId).update(
                    mapOf(
                        "orderStatus" to OrderStatus.DELIVERED.name,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "deliveredAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                _statusUpdateResult.value = Resource.Success(true)
                fetchOrderById(orderId) // Refresh the order data
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error marking as delivered", e)
                _statusUpdateResult.value = Resource.Error("Failed to mark as delivered: ${e.message}")
            }
        }
    }

    private suspend fun fetchPrescriptionsForOrder(document: DocumentSnapshot): List<Prescription> {
        return try {
            val prescriptions = document.get("prescriptions") as? List<Map<String, Any>> ?: emptyList()
            prescriptions.mapNotNull { prescriptionMap ->
                Prescription(
                    id = prescriptionMap["id"] as? String ?: "",
                    prescriptionImageUrl = prescriptionMap["prescriptionImageUrl"] as? String ?: "",
                    status = prescriptionMap["status"] as? String ?: "pending",
                    timestamp = (prescriptionMap["timestamp"] as? Long) ?: 0L,
                    userId = prescriptionMap["userId"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error parsing prescriptions", e)
            emptyList()
        }
    }

    private fun parseAddress(addressMap: Map<String, Any>?): Address {
        return if (addressMap != null) {
            Address(
                addressTitle = addressMap["addressTitle"] as? String ?: "",
                fullName = addressMap["fullName"] as? String ?: "",
                street = addressMap["street"] as? String ?: "",
                phone = addressMap["phone"] as? String ?: "",
                city = addressMap["city"] as? String ?: "",
                state = addressMap["state"] as? String ?: ""
            )
        } else {
            Address()
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _statusUpdateResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                // Validate status transition
                val currentOrder = _currentOrder.value?.data
                if (currentOrder != null && !isValidStatusTransition(currentOrder.status, newStatus)) {
                    _statusUpdateResult.value = Resource.Error("Invalid status transition from ${currentOrder.status} to $newStatus")
                    return@launch
                }

                ordersCollection.document(orderId).update(
                    mapOf(
                        "orderStatus" to newStatus.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                _statusUpdateResult.value = Resource.Success(true)
                fetchOrderById(orderId) // Refresh the order data
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error updating order status", e)
                _statusUpdateResult.value = Resource.Error("Failed to update order status: ${e.message}")
            }
        }
    }

    private fun isValidStatusTransition(current: OrderStatus, next: OrderStatus): Boolean {
        // Define valid transitions
        return when (current) {
            OrderStatus.ORDERED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED
            OrderStatus.PROCESSING -> next == OrderStatus.READY_FOR_DELIVERY || next == OrderStatus.CANCELLED
            OrderStatus.READY_FOR_DELIVERY -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED
            OrderStatus.SHIPPED -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED
            OrderStatus.DELIVERED -> false // Can't transition from DELIVERED
            OrderStatus.CANCELLED -> false // Can't transition from CANCELLED
            OrderStatus.PENDING -> true // Can transition from PENDING to any state
        }
    }

    fun searchOrders(query: String) {
        if (query.isEmpty()) {
            applyFiltersAndSorting()
            return
        }

        _orders.value = allOrders.filter {
            it.orderId.contains(query, true) ||
                    it.address.fullName.contains(query, true) ||
                    it.address.phone.contains(query, true)
        }
    }

    fun filterOrders(status: OrderStatus?) {
        currentFilter.value = status
        applyFiltersAndSorting()
    }

    fun sortOrders(sortOrder: SortOrder) {
        _orders.value = when (sortOrder) {
            SortOrder.ORDER_ID_ASC -> _orders.value?.sortedBy { it.orderId }
            SortOrder.ORDER_ID_DESC -> _orders.value?.sortedByDescending { it.orderId }
            SortOrder.NEWEST_FIRST -> _orders.value?.sortedByDescending { it.createdAt }
            SortOrder.OLDEST_FIRST -> _orders.value?.sortedBy { it.createdAt }
            SortOrder.PRICE_HIGH_TO_LOW -> _orders.value?.sortedByDescending { it.totalPrice }
            SortOrder.PRICE_LOW_TO_HIGH -> _orders.value?.sortedBy { it.totalPrice }
            SortOrder.PHARMACY_NAME -> _orders.value?.sortedBy { order ->
                order.products.firstOrNull()?.pharmacyName ?: ""
            }
        }
    }

    private fun applyFiltersAndSorting() {
        _orders.value = allOrders.filter { order ->
            currentFilter.value?.let { order.status == it } ?: true
        }.sortedByDescending { it.createdAt }
    }

    private suspend fun fetchPharmacists(): Map<String, Pharmacist> {
        return try {
            pharmacistsCollection.get().await()
                .documents.associate { it.id to (it.toObject<Pharmacist>() ?: Pharmacist()) }
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error fetching pharmacists", e)
            emptyMap()
        }
    }

    private suspend fun fetchOrdersWithPharmacyDetails(): List<Order> {
        return try {
            val query = currentPharmacistId?.let {
                ordersCollection.whereArrayContains("pharmacistIds", it)
            } ?: ordersCollection

            query.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        parseOrderDocument(doc)
                    } catch (e: Exception) {
                        Log.e("OrderViewModel", "Error parsing order ${doc.id}", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error fetching orders", e)
            emptyList()
        }
    }
}