package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.OrderItem
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.databinding.ItemProductBinding

class OrderProductsAdapter(
    private var products: List<OrderItem>
) : RecyclerView.Adapter<OrderProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            // Load the first image if available
            product.images.firstOrNull()?.let { imageUrl ->
                Glide.with(ivProductImage.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_medicine)
                    .error(R.drawable.ic_error)
                    .into(ivProductImage)
            } ?: run {
                ivProductImage.setImageResource(R.drawable.ic_medicine)
            }

            tvProductName.text = product.productName
            tvQuantity.text = "Qty: ${product.quantity}"
            tvPrice.text = "$${product.unitPrice} each" // Simplified price display
            tvTotalPrice.text = "Total: $${product.totalPrice}" // Added total price
            tvPharmacy.text = "Pharmacy: ${product.pharmacyName}"

            // Set product status
            tvProductStatus.text = when (product.status) {
                OrderStatus.PENDING.name -> "Pending"
                OrderStatus.PROCESSING.name -> "Processing"
                OrderStatus.SHIPPED.name -> "Shipped"
                OrderStatus.DELIVERED.name -> "Delivered"
                else -> "Unknown"
            }

            // Set status color
            val (bgColor, textColor) = when (product.status) {
                OrderStatus.PENDING.name -> Pair(R.color.status_pending_light, R.color.status_pending)
                OrderStatus.PROCESSING.name -> Pair(R.color.status_processing_light, R.color.status_processing)
                OrderStatus.SHIPPED.name -> Pair(R.color.status_shipped_light, R.color.status_shipped)
                OrderStatus.DELIVERED.name -> Pair(R.color.status_delivered_light, R.color.status_delivered)
                else -> Pair(R.color.status_unknown_light, R.color.status_unknown)
            }

            tvProductStatus.setBackgroundResource(bgColor)
            tvProductStatus.setTextColor(ContextCompat.getColor(root.context, textColor))
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<OrderItem>) {
        products = newProducts
        notifyDataSetChanged()
    }
}