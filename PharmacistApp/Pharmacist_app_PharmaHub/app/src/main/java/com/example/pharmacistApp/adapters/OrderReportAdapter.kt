package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.OrderReportItem
import com.example.pharmacistApp.databinding.ItemOrderReportBinding
import java.text.NumberFormat
import java.util.Locale

class OrderReportAdapter(
    private val orders: List<OrderReportItem>,
    private val onItemClick: (OrderReportItem) -> Unit
) : RecyclerView.Adapter<OrderReportAdapter.OrderReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderReportViewHolder {
        val binding = ItemOrderReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderReportViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderReportViewHolder(private val binding: ItemOrderReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderReportItem) {
            // Format order ID to be shorter
            val shortOrderId = "Order #${order.orderId.take(8)}"
            binding.tvOrderId.text = shortOrderId
            binding.tvDate.text = order.date
            binding.tvCustomerName.text = order.customerName

            // Format status and set appropriate background
            binding.tvStatus.text = order.status
            val statusBackground = when (order.status) {
                "DELIVERED" -> R.drawable.bg_status_delivered
                "PENDING" -> R.drawable.bg_status_pending
                "PROCESSING" -> R.drawable.bg_status_processing
                "CANCELLED" -> R.drawable.bg_status_cancelled
                else -> R.drawable.bg_status_pending
            }
            binding.tvStatus.setBackgroundResource(statusBackground)

            // Format currency
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PK"))
            numberFormat.currency = java.util.Currency.getInstance("PKR")
            val formattedAmount = numberFormat.format(order.total)
                .replace("PKR", "Rs.")
                .replace(".00", "")

            binding.tvAmount.text = formattedAmount

            // Set click listener on the item view
            binding.root.setOnClickListener {
                onItemClick(order)
            }
        }
    }
}