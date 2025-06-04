package com.example.pharmacistApp.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Pharmacist
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PharmacistsAdapter(
    private val onItemClick: (Pharmacist) -> Unit,
    private val onRemoveClick: (Pharmacist) -> Unit,
    private val onEditClick: ((Pharmacist) -> Unit)? = null
) : ListAdapter<Pharmacist, PharmacistsAdapter.ViewHolder>(PharmacistDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPharmacyName: TextView = itemView.findViewById(R.id.tvPharmacyName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvOrderStats: TextView = itemView.findViewById(R.id.tvOrderStats)
        private val ivProfilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnOptions: MaterialButton = itemView.findViewById(R.id.btnOptions)

        fun bind(pharmacist: Pharmacist) {
            // Set name - use displayName property if available
            tvName.text = pharmacist.displayName

            // Set pharmacy name
            tvPharmacyName.text = pharmacist.pharmacyName

            // Set location using the helper property
            tvLocation.text = pharmacist.completeLocation

            // Load profile image if available
            pharmacist.profileImageUrl?.let { imageUrl ->
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_empty_users)
                    .into(ivProfilePic)
            } ?: run {
                // Set default profile image
                ivProfilePic.setImageResource(R.drawable.ic_user)
            }

            // Show order statistics
            tvOrderStats.text = "Orders: ${pharmacist.ordersCompleted} completed · " +
                    "${pharmacist.ordersPending} pending · ${pharmacist.ordersOngoing} ongoing"

            // Set status indicator and text
            val context = itemView.context
            when {
                pharmacist.isSuspended -> {
                    statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.orange))
                    tvStatus.text = "Suspended"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orange))

                    // Show suspension end date if available
                    pharmacist.suspensionEnd?.let { endDate ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        tvStatus.text = "Suspended until ${dateFormat.format(endDate)}"
                    }

                    // Set card stroke color
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.orange)
                    cardView.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                }
                pharmacist.status -> {
                    statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                    tvStatus.text = "Active"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
                    cardView.strokeWidth = 0
                }
                else -> {
                    statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
                    tvStatus.text = "Inactive"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red))
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.light_gray)
                    cardView.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                }
            }

            // Set click listeners
            itemView.setOnClickListener { onItemClick(pharmacist) }
            btnOptions.setOnClickListener { view -> showOptionsMenu(view, pharmacist) }
        }

        private fun showOptionsMenu(view: View, pharmacist: Pharmacist) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_pharmacist_item, popup.menu)

            // Dynamically modify menu based on pharmacist status



            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_remove -> {
                        onRemoveClick(pharmacist)
                        true
                    }

                    // Additional menu options can be handled here
                    else -> false
                }
            }
            popup.show()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacist_with_options, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}