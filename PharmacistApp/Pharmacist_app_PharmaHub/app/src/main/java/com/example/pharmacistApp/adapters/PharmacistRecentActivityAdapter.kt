package com.example.pharmacistApp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.RecentActivity
import java.text.SimpleDateFormat
import java.util.Locale

class PharmacistRecentActivityAdapter(
    private val onItemClick: (RecentActivity) -> Unit = {}
) : ListAdapter<RecentActivity, PharmacistRecentActivityAdapter.ActivityViewHolder>(RecentActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pharmacist_recent_iitem, parent, false)
        return ActivityViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActivityViewHolder(
        itemView: View,
        private val onItemClick: (RecentActivity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val iconImage: ImageView = itemView.findViewById(R.id.activityIcon)
        private val titleText: TextView = itemView.findViewById(R.id.activityTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.activityDescription)
        private val timeText: TextView = itemView.findViewById(R.id.activityTime)
        private val newIndicator: View = itemView.findViewById(R.id.newIndicator)

        fun bind(activity: RecentActivity) {
            with(activity) {
                titleText.text = title
                descriptionText.text = description
                newIndicator.visibility = if (isNew) View.VISIBLE else View.GONE

                // Format timestamp
                timeText.text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    .format(timestamp)

                // Set icon and styling based on activity type
                setActivityTypeStyling(type)

                // Set click listener
                itemView.setOnClickListener { onItemClick(activity) }
            }
        }

        private fun setActivityTypeStyling(type: PharmaActivityType) {
            val (iconRes, bgRes, tintColor) = when (type) {
                PharmaActivityType.NEW_USER -> Triple(
                    R.drawable.ic_user,
                    R.drawable.circle_success_light,
                    R.color.colorSuccess
                )
                PharmaActivityType.NEW_ORDER -> Triple(
                    R.drawable.ic_orders,
                    R.drawable.circle_primary_light,
                    R.color.primary_color
                )
                PharmaActivityType.PHARMACY_APPLICATION -> Triple(
                    R.drawable.ic_medicine,
                    R.drawable.circle_info_light,
                    R.color.colorInfo
                )
                PharmaActivityType.COMPLAINT -> Triple(
                    R.drawable.ic_customer_support,
                    R.drawable.circle_warning_light,
                    R.color.colorWarning
                )
                PharmaActivityType.LOW_STOCK -> Triple(
                    R.drawable.ic_inventory,
                    R.drawable.circle_warning_light,
                    R.color.colorError
                )
                PharmaActivityType.NEW_PRESCRIPTION -> Triple(
                    R.drawable.ic_prescriptions,
                    R.drawable.circle_purple_light,
                    R.color.purple_500
                )
                else -> Triple(
                    R.drawable.ic_notifications,
                    R.drawable.circle_primary_light,
                    R.color.secondary_color
                )
            }

            iconImage.setImageResource(iconRes)
            iconImage.background = ContextCompat.getDrawable(itemView.context, bgRes)
            iconImage.imageTintList = ContextCompat.getColorStateList(itemView.context, tintColor)
        }
    }

    private class RecentActivityDiffCallback : DiffUtil.ItemCallback<RecentActivity>() {
        override fun areItemsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem == newItem
        }
    }
}