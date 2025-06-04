package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.databinding.ItemPharmacistApplicationBinding
import java.text.SimpleDateFormat
import java.util.*

class PharmacistApplicationsAdapter(
    private val onApproveClick: (Pharmacist) -> Unit,
    private val onRejectClick: (Pharmacist) -> Unit
) : ListAdapter<Pharmacist, PharmacistApplicationsAdapter.PharmacistViewHolder>(PharmacistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacistViewHolder {
        val binding = ItemPharmacistApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PharmacistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PharmacistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PharmacistViewHolder(
        private val binding: ItemPharmacistApplicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pharmacist: Pharmacist) {
            with(binding) {
                // Bind basic information
                tvName.text = "${pharmacist.firstName} ${pharmacist.lastName}"
                tvEmail.text = pharmacist.email
                tvPharmacyName.text = pharmacist.pharmacyName
                tvLicenseNumber.text = pharmacist.licenseNumber

                // Format address
                tvAddress.text = listOfNotNull(
                    pharmacist.address,
                    pharmacist.city,
                    pharmacist.state,
                    pharmacist.zipCode
                ).joinToString(", ")

                // Format phone number
                tvPhone.text = pharmacist.phoneNumber.takeIf { it.isNotEmpty() }?.let {
                    "Phone: ${formatPhoneNumber(it)}"
                } ?: "Phone: Not provided"

                // Handle suspension status
                if (pharmacist.isSuspended) {
                    chipStatus.apply {
                        text = context.getString(R.string.suspended)
                        setChipBackgroundColorResource(R.color.g_red)
                        setTextColor(ContextCompat.getColor(context, R.color.g_red))
                        visibility = View.VISIBLE
                    }

                    layoutSuspendedInfo.visibility = View.VISIBLE
                    pharmacist.suspensionEnd?.let { date ->
                        tvSuspendedDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                    }
                } else {
                    chipStatus.visibility = View.GONE
                    layoutSuspendedInfo.visibility = View.GONE
                }

                // Configure action buttons
                btnApprove.apply {
                    text = context.getString(
                        if (pharmacist.isSuspended) R.string.reinstate else R.string.approve
                    )
                    setOnClickListener { onApproveClick(pharmacist) }
                }

                btnReject.setOnClickListener { onRejectClick(pharmacist) }
            }
        }

        private fun formatPhoneNumber(phone: String): String {
            return if (phone.length == 10) {
                "(${phone.substring(0, 3)}) ${phone.substring(3, 6)}-${phone.substring(6)}"
            } else {
                phone
            }
        }
    }

    class PharmacistDiffCallback : DiffUtil.ItemCallback<Pharmacist>() {
        override fun areItemsTheSame(oldItem: Pharmacist, newItem: Pharmacist): Boolean {
            return oldItem.pharmacistId == newItem.pharmacistId
        }

        override fun areContentsTheSame(oldItem: Pharmacist, newItem: Pharmacist): Boolean {
            return oldItem == newItem
        }
    }
}