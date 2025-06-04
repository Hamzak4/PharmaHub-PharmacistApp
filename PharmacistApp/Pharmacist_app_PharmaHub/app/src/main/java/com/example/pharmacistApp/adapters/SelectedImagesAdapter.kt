package com.example.pharmacistApp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.databinding.ItemSelectedImageBinding

class SelectedImagesAdapter : ListAdapter<Uri, SelectedImagesAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(val binding: ItemSelectedImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = getItem(position)
        Glide.with(holder.itemView)
            .load(uri)
            .centerCrop()
            .into(holder.binding.ivSelectedImage)

        holder.binding.btnRemove.setOnClickListener {
            val updatedList = currentList.toMutableList().apply { removeAt(position) }
            submitList(updatedList)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    }
}