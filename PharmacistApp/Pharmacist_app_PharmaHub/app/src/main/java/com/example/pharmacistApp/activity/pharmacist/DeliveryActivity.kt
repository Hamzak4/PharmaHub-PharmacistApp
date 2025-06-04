package com.example.pharmacistApp.activity.pharmacist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Address
import com.example.pharmacistApp.data.CourierInfo
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.databinding.ActivityDeliveryBinding
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.OrderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DeliveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryBinding
    private val viewModel: OrderViewModel by viewModels()
    private var orderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: run {
            showErrorState("Invalid order ID")
            finish()
            return
        }

        setupToolbar()
        setupObservers()
        setupClickListeners()
        viewModel.fetchOrderById(orderId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Delivery Details"
        }
    }

    private fun setupObservers() {
        viewModel.currentOrder.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoadingState()
                is Resource.Success -> {
                    hideLoadingState()
                    resource.data?.let { order ->
                        displayOrderDetails(order)
                    } ?: showErrorState("Order not found")
                }
                is Resource.Error -> {
                    showErrorState(resource.message ?: "Error loading order details")
                }

            }
        }

        viewModel.statusUpdateResult.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    hideLoadingState()
                    showSuccessMessage("Status updated successfully")
                }
                is Resource.Error -> {
                    hideLoadingState()
                    showErrorMessage(result.message ?: "Failed to update status")
                }
                is Resource.Loading -> showLoadingState()

            }
        }
    }

    private fun displayOrderDetails(order: Order) {
        with(binding) {
            // Order Info
            tvOrderId.text = "Order #${order.orderId}"
            tvOrderDate.text = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(order.createdAt)
            tvPaymentStatus.text = if (order.paymentMethod == "COD") "Cash on Delivery" else "Paid Online"

            // Delivery Status
            updateDeliveryStatusUI(order.status)

            // Customer Info
            tvCustomerName.text = order.address.fullName
            tvCustomerPhone.text = order.address.phone
            tvCustomerAddress.text = buildAddressString(order.address)

            // Courier Info
            setupCourierInfo(order.courierInfo)

            // Delivery Instructions
            tvDeliveryInstructions.text = order.deliveryInstructions.ifEmpty {
                "No specific delivery instructions"
            }
        }
    }

    private fun buildAddressString(address: Address): String {
        return listOfNotNull(
            address.street,
            address.city,
            address.state
        ).joinToString(", ")
    }

    private fun setupCourierInfo(courierInfo: CourierInfo?) {
        with(binding) {
            if (courierInfo != null) {
                layoutCourierInfo.visibility = View.VISIBLE
                tvCourierName.text = courierInfo.name
                tvCourierPhone.text = courierInfo.phone
                tvTrackingNumber.text = courierInfo.trackingNumber ?: "Not available"

                Glide.with(this@DeliveryActivity)
                    .load(courierInfo.photoUrl)
                    .placeholder(R.drawable.ic_delivery_person)
                    .error(R.drawable.ic_delivery_person)
                    .circleCrop()
                    .into(ivCourierPhoto)

                btnContactCourier.isEnabled = courierInfo.phone.isNotBlank()
                btnTrackOrder.isEnabled = courierInfo.trackingUrl.isNotBlank()
            } else {
                layoutCourierInfo.visibility = View.GONE
            }
        }
    }

    private fun updateDeliveryStatusUI(status: OrderStatus) {
        with(binding) {
            tvDeliveryStatus.text = status.displayName
            tvDeliveryStatus.setTextColor(ContextCompat.getColor(this@DeliveryActivity, getStatusColor(status)))
            tvDeliveryStatus.setBackgroundResource(status.iconRes)

            btnUpdateStatus.text = when (status) {
                OrderStatus.ORDERED -> "Start Processing"
                OrderStatus.PROCESSING -> "Mark as Ready for Delivery"
                OrderStatus.READY_FOR_DELIVERY -> "Assign Courier"
                OrderStatus.SHIPPED -> "Mark as Delivered"
                else -> ""
            }

            btnUpdateStatus.visibility = if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun getStatusColor(status: OrderStatus): Int {
        return when (status) {
            OrderStatus.ORDERED -> R.color.grey_600
            OrderStatus.PROCESSING -> R.color.orange
            OrderStatus.READY_FOR_DELIVERY -> R.color.g_blue
            OrderStatus.SHIPPED -> R.color.purple_200
            OrderStatus.DELIVERED -> R.color.green
            OrderStatus.CANCELLED -> R.color.red
            else -> R.color.grey_600
        }
    }

    private fun setupClickListeners() {
        binding.btnUpdateStatus.setOnClickListener {
            showStatusUpdateDialog()
        }

        binding.btnContactCustomer.setOnClickListener {
            viewModel.currentOrder.value?.data?.address?.phone?.let { phone ->
                startActivity(Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                })
            }
        }

        binding.btnContactCourier.setOnClickListener {
            viewModel.currentOrder.value?.data?.courierInfo?.phone?.let { phone ->
                startActivity(Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                })
            }
        }

        binding.btnTrackOrder.setOnClickListener {
            viewModel.currentOrder.value?.data?.courierInfo?.trackingUrl?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                })
            }
        }

        binding.btnRetry.setOnClickListener {
            viewModel.fetchOrderById(orderId)
        }
    }

    private fun showStatusUpdateDialog() {
        val currentStatus = viewModel.currentOrder.value?.data?.status ?: return

        val nextStatus = when (currentStatus) {
            OrderStatus.ORDERED -> OrderStatus.PROCESSING
            OrderStatus.PROCESSING -> OrderStatus.READY_FOR_DELIVERY
            OrderStatus.READY_FOR_DELIVERY -> OrderStatus.SHIPPED
            OrderStatus.SHIPPED -> OrderStatus.DELIVERED
            else -> null
        }

        nextStatus?.let { status ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Update Order Status")
                .setMessage("Change order status to ${status.displayName}?")
                .setPositiveButton("Confirm") { _, _ ->
                    viewModel.updateOrderStatus(orderId, status)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } ?: run {
            showErrorMessage("Order is already in its final status")
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
        binding.contentGroup.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentGroup.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun showSuccessMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"

        fun newIntent(context: Context, orderId: String): Intent {
            return Intent(context, DeliveryActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
            }
        }
    }
}