package com.example.blueprinttracker.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blueprinttracker.data.BucketAllocation
import com.example.blueprinttracker.databinding.ItemBucketBinding
import java.text.NumberFormat
import java.util.Locale

class BucketAdapter(
    private val onBucketClick: (BucketAllocation) -> Unit
) : ListAdapter<BucketAllocation, BucketAdapter.BucketViewHolder>(BucketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BucketViewHolder {
        val binding = ItemBucketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BucketViewHolder(binding, onBucketClick)
    }

    override fun onBindViewHolder(holder: BucketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BucketViewHolder(
        private val binding: ItemBucketBinding,
        private val onBucketClick: (BucketAllocation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(allocation: BucketAllocation) {
            binding.apply {
                textBucketName.text = allocation.bucket.name
                textStockCount.text = "${allocation.stockCount} stocks"
                textBucketAmount.text = currencyFormatter.format(allocation.currentValue)
                
                val percentageText = String.format(
                    "%.1f%% / %.1f%%",
                    allocation.currentPercentage,
                    allocation.targetPercentage
                )
                textBucketPercentage.text = percentageText
                
                try {
                    bucketColorIndicator.setBackgroundColor(Color.parseColor(allocation.bucket.color))
                } catch (e: Exception) {
                    // Fallback if color string is invalid
                }

                progressAllocation.progress = allocation.currentPercentage.toInt()
                
                root.setOnClickListener { onBucketClick(allocation) }
            }
        }
    }

    private class BucketDiffCallback : DiffUtil.ItemCallback<BucketAllocation>() {
        override fun areItemsTheSame(oldItem: BucketAllocation, newItem: BucketAllocation): Boolean {
            return oldItem.bucket.bucketId == newItem.bucket.bucketId
        }

        override fun areContentsTheSame(oldItem: BucketAllocation, newItem: BucketAllocation): Boolean {
            return oldItem == newItem
        }
    }
}
