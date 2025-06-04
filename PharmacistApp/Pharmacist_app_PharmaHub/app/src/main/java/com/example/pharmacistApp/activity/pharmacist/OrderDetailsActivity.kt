package com.example.pharmacistApp.activity.pharmacist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.adapters.OrderProductsAdapter
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.databinding.ActivityOrderDetailsBinding
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.OrderViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding
    private val viewModel: OrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get order ID from intent
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: run {
            finish()
            return
        }

        // Setup RecyclerViews
        setupRecyclerViews()

        // Observe order details
        viewModel.currentOrder.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading state
                    binding.progressBar.visibility = View.VISIBLE
                    binding.contentGroup.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                }
                is Resource.Success -> {
                    // Hide loading and error, show content
                    binding.progressBar.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.contentGroup.visibility = View.VISIBLE

                    resource.data?.let { order ->
                        displayOrderDetails(order)
                    }
                }
                is Resource.Error -> {
                    // Show error state
                    binding.progressBar.visibility = View.GONE
                    binding.contentGroup.visibility = View.GONE
                    binding.errorView.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = resource.message ?: "Unknown error occurred"
                }
                // Remove the Unspecified case entirely
            }
        }

        // Fetch order details
        viewModel.fetchOrderById(orderId)

        // Setup button click listeners
        binding.btnUpdateStatus.setOnClickListener {
            // Show status update dialog
        }

        binding.btnContactCustomer.setOnClickListener {
            // Navigate to DeliveryActivity
            val intent = DeliveryActivity.newIntent(this, orderId)
            startActivity(intent)
        }

    }

    private fun setupRecyclerViews() {
        // Prescriptions RecyclerView


        // Products RecyclerView
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailsActivity)
            adapter = OrderProductsAdapter(emptyList())
        }
    }

    private fun displayOrderDetails(order: Order) {
        // Order summary
        binding.tvOrderId.text = "Order #${order.orderId}"
        binding.tvOrderStatus.text = "Status: ${order.status.displayName}"
        binding.tvOrderDate.text = "Date: ${order.date}"
        binding.tvTotalPrice.text = "Total: $${order.totalPrice}"
        binding.tvPaymentMethod.text = "Payment: ${order.paymentMethod}"
        binding.tvDeliveryInstructions.text = "Instructions: ${order.deliveryInstructions}"

        // Customer info
        binding.tvCustomerName.text = order.address.fullName
        binding.tvCustomerPhone.text = "Phone: ${order.address.phone}"
        binding.tvCustomerAddress.text = buildString {
            append(order.address.street)
            append(", ")
            append(order.address.city)
            append(", ")
            append(order.address.state)
        }

        // Update RecyclerViews

        (binding.rvProducts.adapter as OrderProductsAdapter).updateProducts(order.products)
    }

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"

        fun newIntent(context: Context, orderId: String): Intent {
            return Intent(context, OrderDetailsActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
            }
        }
    }
}