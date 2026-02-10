package com.example.blueprinttracker.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blueprinttracker.data.StockTransaction
import com.example.blueprinttracker.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class TransactionAdapter : ListAdapter<StockTransaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StockTransaction) {
            binding.textTransactionType.text = item.type
            binding.textTransactionDate.text = dateFormatter.format(Date(item.timestamp))
            
            // Format amount as absolute value with +/- sign
            val formattedAmount = currencyFormatter.format(abs(item.amount))
            if (item.amount >= 0) {
                binding.textTransactionAmount.text = "+$formattedAmount"
                binding.textTransactionAmount.setTextColor(Color.parseColor("#388E3C")) // Green
                binding.textTransactionType.setTextColor(Color.parseColor("#388E3C"))
            } else {
                binding.textTransactionAmount.text = "-$formattedAmount"
                binding.textTransactionAmount.setTextColor(Color.RED)
                binding.textTransactionType.setTextColor(Color.RED)
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<StockTransaction>() {
        override fun areItemsTheSame(oldItem: StockTransaction, newItem: StockTransaction): Boolean {
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: StockTransaction, newItem: StockTransaction): Boolean {
            return oldItem == newItem
        }
    }
}
