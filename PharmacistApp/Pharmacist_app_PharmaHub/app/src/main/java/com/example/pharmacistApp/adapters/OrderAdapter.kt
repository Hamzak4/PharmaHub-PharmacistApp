package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private var orders: List<Order>,
    private val onItemClick: (Order) -> Unit,
    private val onDetailsClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    inner class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) = with(binding) {
            tvOrderId.text = root.context.getString(R.string.order_id_format, order.id)
            tvCustomerName.text = order.customerName
            tvOrderDate.text = dateFormat.format(order.createdAt)
            tvTotalPrice.text = root.context.getString(R.string.price_format, order.totalPrice)
            tvStatus.text = order.status.name
            tvStatus.setBackgroundResource(getStatusBackground(order.status))
            btnDetails.setOnClickListener { onDetailsClick(order) }
            root.setOnClickListener { onItemClick(order) }
        }

        private fun getStatusBackground(status: OrderStatus): Int = when (status) {
            OrderStatus.PENDING -> R.drawable.bg_status_pending
            OrderStatus.PROCESSING -> R.drawable.bg_status_waiting
            OrderStatus.ORDERED -> R.drawable.bg_status_approved
            OrderStatus.CANCELLED -> R.drawable.bg_status_rejected
            OrderStatus.SHIPPED -> R.drawable.bg_status_prepared
            OrderStatus.ORDERED -> R.drawable.bg_status_out_for_delivery
            OrderStatus.DELIVERED -> R.drawable.bg_status_delivered
            OrderStatus.READY_FOR_DELIVERY->R.drawable.bg_status_ready
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        OrderViewHolder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) = holder.bind(orders[position])
    override fun getItemCount() = orders.size
    fun updateOrders(newOrders: List<Order>) { orders = newOrders; notifyDataSetChanged() }
}