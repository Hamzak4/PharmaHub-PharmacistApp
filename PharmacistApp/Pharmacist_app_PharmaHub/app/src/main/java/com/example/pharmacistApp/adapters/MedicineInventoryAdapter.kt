package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.databinding.ItemMedicineInventoryBinding
import java.text.NumberFormat
import java.util.*

class MedicineInventoryAdapter(private val listener: OnMedicineActionListener) :
    ListAdapter<Product, MedicineInventoryAdapter.MedicineViewHolder>(MedicineComparator()) {

    interface OnMedicineActionListener {
        fun onMedicineEdit(product: Product)
        fun onMedicineDelete(product: Product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicineViewHolder(private val binding: ItemMedicineInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                // Set medicine name and generic name
                txtMedicineName.text = product.name
                txtGenericName.text = product.genericName

                // Set category
                txtCategory.text = product.category

                // Set price with currency formatting
                val priceFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                txtPrice.text = priceFormat.format(product.price)

                // Set stock quantity and status
                val stockText = "${product.quantity} units"
                txtStock.text = stockText

                // Determine stock status based solely on quantity for consistency
                when {
                    product.quantity <= 0 -> {
                        txtStockStatus.text = "Out of Stock"
                        txtStockStatus.background = ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.bg_stock_badge_out
                        )
                    }
                    product.quantity <= 10 -> {
                        txtStockStatus.text = "Low Stock"
                        txtStockStatus.background = ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.bg_stock_badge_low
                        )
                    }
                    else -> {
                        txtStockStatus.text = "In Stock"
                        txtStockStatus.background = ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.bg_stock_badge_in
                        )
                    }
                }

                // Load medicine image if available
                if (product.images.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(product.images[0])
                        .placeholder(R.drawable.ic_medicine)
                        .error(R.drawable.ic_medicine)
                        .into(imgMedicine)
                } else {
                    imgMedicine.setImageResource(R.drawable.ic_medicine)
                }

                // Setup action button
                btnActions.setOnClickListener { showPopupMenu(it, product) }
            }
        }

        private fun showPopupMenu(view: View, product: Product) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.menu_medicine_actions)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        listener.onMedicineEdit(product)
                        true
                    }
                    R.id.action_delete -> {
                        listener.onMedicineDelete(product)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    class MedicineComparator : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}