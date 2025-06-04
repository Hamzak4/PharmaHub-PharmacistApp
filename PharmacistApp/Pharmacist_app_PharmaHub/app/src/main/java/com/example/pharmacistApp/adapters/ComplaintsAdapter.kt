package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Complaint
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComplaintsAdapter(
    private var complaints: List<Complaint>,
    private val onRespondClick: (Complaint) -> Unit,
    private val isAdminView: Boolean = false,
    private val onViewResponsesClick: (Complaint) -> Unit = {}
) : RecyclerView.Adapter<ComplaintsAdapter.ComplaintViewHolder>() {

    inner class ComplaintViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Using safe property initialization with lazy loading
        private val tvUserName: TextView by lazy { itemView.findViewById(R.id.tvUserName) }
        private val tvUserEmail: TextView by lazy { itemView.findViewById(R.id.tvUserEmail) }
        private val tvComplaintText: TextView by lazy { itemView.findViewById(R.id.tvComplaintText) }

        // Updated to use chipStatus instead of tvStatus
        private val chipStatus: Chip by lazy {
            itemView.findViewById<Chip>(R.id.chipStatus).apply {
                // Default setup in case binding fails
                setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            }
        }

        private val tvDate: TextView by lazy { itemView.findViewById(R.id.tvDate) }
        private val btnRespond: MaterialButton by lazy { itemView.findViewById(R.id.btnRespond) }
        private val btnViewResponses: MaterialButton by lazy { itemView.findViewById(R.id.btnViewResponses) }

        fun bind(complaint: Complaint) {
            tvUserName.text = complaint.userName.ifEmpty { "Anonymous" }
            tvUserEmail.text = complaint.userEmail
            tvComplaintText.text = complaint.text

            // Safe date formatting with proper null and type checking
            tvDate.text = formatDate(complaint.timestamp)

            // Update for Chip component instead of TextView
            chipStatus.text = complaint.status.uppercase()

            // Set the chip color based on status
            when (complaint.status.lowercase()) {
                "pending" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.status_pending)
                }
                "responded" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.g_green)
                }
                else -> {
                    chipStatus.setChipBackgroundColorResource(R.color.g_red)
                }
            }

            // Show respond button only for pending complaints in admin view
            btnRespond.visibility = if (isAdminView && complaint.status == "pending") {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Show view responses button if there are responses
            btnViewResponses.visibility = if (complaint.responses.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnRespond.setOnClickListener { onRespondClick(complaint) }
            btnViewResponses.setOnClickListener { onViewResponsesClick(complaint) }
        }

        // Helper method to safely format dates from various possible types
        private fun formatDate(timestamp: Any?): String {
            if (timestamp == null) {
                return "No date"
            }

            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

            return try {
                when (timestamp) {
                    is Date -> formatter.format(timestamp)
                    is Long -> formatter.format(Date(timestamp))
                    is com.google.firebase.Timestamp -> formatter.format(timestamp.toDate())
                    is String -> {
                        try {
                            // Try to parse as Long first
                            formatter.format(Date(timestamp.toLong()))
                        } catch (e: NumberFormatException) {
                            // If not a number, return the string as is
                            timestamp
                        }
                    }
                    else -> "Unknown date format"
                }
            } catch (e: Exception) {
                "Date error"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        // This is where the error occurred - we need to make sure we're using the correct layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_complaint, parent, false)

        // Add additional error checking to help diagnose potential issues
        if (view == null) {
            throw IllegalStateException("Failed to inflate layout R.layout.item_complaint")
        }

        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        // Add bounds checking to prevent IndexOutOfBoundsException
        if (position < 0 || position >= complaints.size) {
            return
        }
        holder.bind(complaints[position])
    }

    override fun getItemCount() = complaints.size

    fun updateList(newList: List<Complaint>) {
        complaints = newList
        notifyDataSetChanged()
    }
}