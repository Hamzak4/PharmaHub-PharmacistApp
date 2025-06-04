package com.example.pharmacistApp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.User
import com.example.pharmacistApp.databinding.ItemUserBinding

class UsersAdapter(
    private val onItemClick: (User) -> Unit = {}
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    companion object {
        private const val TAG = "UsersAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        Log.d(TAG, "Creating new view holder")
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        Log.d(TAG, "Binding user at position $position: ${user.id} - ${user.name}")
        holder.bind(user)
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            with(binding) {
                Log.d(TAG, "Binding data for user: ${user.id}")

                // Set user name and email
                tvUserName.text = user.name.ifEmpty { "No name" }
                tvUserEmail.text = user.email.ifEmpty { "No email" }

                // Load user image
                if (user.imagePath.isNotEmpty()) {
                    Log.d(TAG, "Loading image for ${user.id}: ${user.imagePath}")
                    Glide.with(itemView.context)
                        .load(user.imagePath)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivUserImage)
                } else {
                    ivUserImage.setImageResource(R.drawable.ic_profile)
                }

                // Set status
                tvUserStatus.apply {
                    text = if (user.isActive) "Active" else "Inactive"
                    setBackgroundResource(
                        if (user.isActive) R.drawable.bg_status_active
                        else R.drawable.bg_status_inactive
                    )
                }

                // Set click listener
                root.setOnClickListener {
                    Log.d(TAG, "User clicked: ${user.id}")
                    onItemClick(user)
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}