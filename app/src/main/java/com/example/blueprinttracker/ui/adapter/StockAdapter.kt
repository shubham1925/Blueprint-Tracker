package com.example.blueprinttracker.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blueprinttracker.data.StockDetail
import com.example.blueprinttracker.databinding.ItemStockBinding
import java.text.NumberFormat
import java.util.Locale

class StockAdapter(private val onStockClick: (StockDetail) -> Unit) : ListAdapter<StockDetail, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val percentFormatter = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StockViewHolder(private val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StockDetail) {
            binding.root.setOnClickListener { onStockClick(item) }
            binding.textStockSymbol.text = item.stock.symbol
            binding.textStockName.text = item.stock.name
            binding.textStockValue.text = currencyFormatter.format(item.stock.currentValue)
            binding.textStockTarget.text = "Target: ${percentFormatter.format(item.stock.targetPercentage / 100.0)}"
            binding.textStockAllocation.text = "Current: ${percentFormatter.format(item.currentPercentage / 100.0)}"
            
            if (item.isOverAllocated) {
                binding.textStockAllocation.setTextColor(Color.RED)
                binding.imageWarning.visibility = View.VISIBLE
                binding.imageWarning.setColorFilter(Color.RED)
            } else {
                binding.textStockAllocation.setTextColor(Color.parseColor("#388E3C")) // Green
                binding.imageWarning.visibility = View.GONE
            }
        }
    }

    class StockDiffCallback : DiffUtil.ItemCallback<StockDetail>() {
        override fun areItemsTheSame(oldItem: StockDetail, newItem: StockDetail): Boolean {
            return oldItem.stock.stockId == newItem.stock.stockId
        }

        override fun areContentsTheSame(oldItem: StockDetail, newItem: StockDetail): Boolean {
            return oldItem == newItem
        }
    }
}
