package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Prescription

class PrescriptionsAdapter(
    private var prescriptions: List<Prescription>,
    private val onActionClickListener: OnActionClickListener
) : RecyclerView.Adapter<PrescriptionsAdapter.PrescriptionViewHolder>() {

    interface OnActionClickListener {
        fun onApproveClick(prescription: Prescription)
        fun onRejectClick(prescription: Prescription)
    }

    inner class PrescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val prescriptionImage: ImageView = itemView.findViewById(R.id.prescriptionImage)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val productsTextView: TextView = itemView.findViewById(R.id.productsTextView)
        val approveButton: Button = itemView.findViewById(R.id.approveButton)
        val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription, parent, false)
        return PrescriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        val prescription = prescriptions[position]

        // Load prescription image
        Glide.with(holder.itemView.context)
            .load(prescription.prescriptionImageUrl)
            .placeholder(R.drawable.ic_empty_users)
            .error(R.drawable.ic_error)
            .into(holder.prescriptionImage)

        // Set date and status
        holder.dateTextView.text = prescription.formattedDate
        holder.statusTextView.text = getStatusText(prescription.status)

        // Display product details
        displayProductDetails(holder, prescription)

        // Set button states and click listeners
        setupButtons(holder, prescription)
    }

    private fun getStatusText(status: String?): String {
        return when (status) {
            "pending_review" -> "Pending Review"
            "approved" -> "Approved"
            "rejected" -> "Rejected"
            "pending" -> "Pending"
            else -> status?.replace("_", " ")?.capitalize() ?: "Unknown"
        }
    }

    private fun displayProductDetails(holder: PrescriptionViewHolder, prescription: Prescription) {
        val context = holder.itemView.context
        val productDetailsText = StringBuilder(context.getString(R.string.prescribed_products))

        // Create a local immutable copy of the product details
        val productDetails = prescription.productDetails ?: emptyList()

        if (productDetails.isNotEmpty()) {
            productDetails.forEachIndexed { index, product ->
                productDetailsText.append("\n${index + 1}. ${product.name ?: "Unknown Product"}")

                listOf(
                    "Generic" to product.genericName,
                    "Strength" to product.strength,
                    "Form" to product.dosageForm
                ).forEach { (label, value) ->
                    value?.takeIf { it.isNotEmpty() }?.let {
                        productDetailsText.append("\n   • $label: $it")
                    }
                }

                if (index < productDetails.size - 1) {
                    productDetailsText.append("\n")
                }
            }
        } else {
            productDetailsText.append("\n${context.getString(R.string.no_product_details)}")
        }

        holder.productsTextView.text = productDetailsText.toString()
    }

    private fun setupButtons(holder: PrescriptionViewHolder, prescription: Prescription) {
        val isPending = prescription.status == "pending_review" || prescription.status == "pending"
        val context = holder.itemView.context

        with(holder) {
            approveButton.isEnabled = isPending
            rejectButton.isEnabled = isPending

            approveButton.setOnClickListener {
                if (isPending) {
                    onActionClickListener.onApproveClick(prescription)
                } else {
                    Toast.makeText(context, R.string.prescription_already_processed, Toast.LENGTH_SHORT).show()
                }
            }

            rejectButton.setOnClickListener {
                if (isPending) {
                    onActionClickListener.onRejectClick(prescription)
                } else {
                    Toast.makeText(context, R.string.prescription_already_processed, Toast.LENGTH_SHORT).show()
                }
            }

            // Set button colors based on status
            when (prescription.status) {
                "approved" -> {
                    approveButton.setBackgroundColor(context.getColor(R.color.green))
                    rejectButton.setBackgroundColor(context.getColor(R.color.light_gray))
                }
                "rejected" -> {
                    approveButton.setBackgroundColor(context.getColor(R.color.light_gray))
                    rejectButton.setBackgroundColor(context.getColor(R.color.red))
                }
                else -> {
                    approveButton.setBackgroundColor(context.getColor(R.color.green))
                    rejectButton.setBackgroundColor(context.getColor(R.color.red))
                }
            }
        }
    }

    override fun getItemCount(): Int = prescriptions.size

    fun updateData(newPrescriptions: List<Prescription>) {
        prescriptions = newPrescriptions
        notifyDataSetChanged()
    }
}