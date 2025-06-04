package com.example.pharmacistApp.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Prescription
import com.example.pharmacistApp.databinding.ItemPrescriptionOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrescriptionOrderAdapter(
    private val onItemClick: (Prescription) -> Unit
) : ListAdapter<Prescription, PrescriptionOrderAdapter.PrescriptionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val binding = ItemPrescriptionOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrescriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrescriptionViewHolder(
        private val binding: ItemPrescriptionOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(prescription: Prescription) {
            with(binding) {
                // Load prescription image
                Glide.with(root.context)
                    .load(prescription.prescriptionImageUrl)
                    .placeholder(R.drawable.ic_prescriptions)
                    .into(ivPrescription)

                // Set status with colored text
                tvStatus.text = prescription.status?.capitalize() ?: "Pending"
                tvStatus.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        when (prescription.status?.lowercase()) {
                            "approved" -> R.color.green
                            "rejected" -> R.color.red
                            else -> R.color.orange
                        }
                    )
                )

                // Set formatted date
                tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(prescription.timestamp))

                // Set click listener
                root.setOnClickListener { onItemClick(prescription) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Prescription>() {
        override fun areItemsTheSame(oldItem: Prescription, newItem: Prescription): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Prescription, newItem: Prescription): Boolean {
            return oldItem == newItem
        }
    }
}