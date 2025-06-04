package com.example.pharmacistApp.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.ActivityType
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.RecentActivity
import com.example.pharmacistApp.databinding.ItemRecentActivityBinding

class RecentActivityAdapter(
    private val onActivityClicked: (RecentActivity) -> Unit = {}
) : ListAdapter<RecentActivity, RecentActivityAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecentActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentActivity) {
            binding.apply {
                // Set icon based on activity type
                ivActivityIcon.setImageResource(
                    when(item.type) {
                        PharmaActivityType.NEW_USER -> R.drawable.ic_user
                        PharmaActivityType.NEW_ORDER -> R.drawable.ic_cart
                        PharmaActivityType.PHARMACY_APPLICATION -> R.drawable.ic_application
                        PharmaActivityType.COMPLAINT -> R.drawable.ic_error
                        PharmaActivityType.NEW_PRESCRIPTION-> R.drawable.ic_prescriptions
                        PharmaActivityType.OTHER->R.drawable.ic_block
                        PharmaActivityType.LOW_STOCK->R.drawable.bg_stock_badge_out
                    }
                )

                tvActivityTitle.text = item.title
                tvActivityDescription.text = item.description
                tvActivityTime.text = DateUtils.getRelativeTimeSpanString(
                    item.timestamp.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

                // Handle new indicator
                viewNewIndicator.visibility = if (item.isNew) View.VISIBLE else View.GONE

                // Add ripple effect for better UX
                root.isClickable = true
                root.isFocusable = true

                // Set click listener
                root.setOnClickListener {
                    onActivityClicked(item)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecentActivity>() {
        override fun areItemsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem == newItem
        }
    }
}