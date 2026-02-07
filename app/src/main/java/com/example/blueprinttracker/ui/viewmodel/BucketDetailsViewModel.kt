package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.blueprinttracker.data.BucketDetailSummary
import com.example.blueprinttracker.data.Stock
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BucketDetailsUiState {
    object Loading : BucketDetailsUiState
    data class Success(val summary: BucketDetailSummary) : BucketDetailsUiState
    data class Error(val message: String) : BucketDetailsUiState
}

class BucketDetailsViewModel(
    private val repository: PortfolioRepository,
    private val bucketId: Long
) : ViewModel() {

    val uiState: StateFlow<BucketDetailsUiState> = repository.getBucketDetailSummary(bucketId)
        .map { summary ->
            if (summary != null) {
                BucketDetailsUiState.Success(summary)
            } else {
                BucketDetailsUiState.Error("Bucket not found")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BucketDetailsUiState.Loading
        )

    fun updateStock(symbol: String, delta: Double, isBuy: Boolean) {
        viewModelScope.launch {
            val stocks = repository.getStocksByBucket(bucketId).first()
            val existingStock = stocks.find { it.symbol.equals(symbol, ignoreCase = true) }

            if (existingStock != null) {
                val newValue = if (isBuy) {
                    existingStock.currentValue + delta
                } else {
                    existingStock.currentValue - delta
                }
                repository.updateStock(existingStock.copy(currentValue = newValue.coerceAtLeast(0.0)))
            } else {
                if (isBuy) {
                    val newStock = Stock(
                        bucketId = bucketId,
                        symbol = symbol.uppercase(),
                        name = symbol.uppercase(),
                        currentValue = delta,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.addStock(newStock)
                }
            }
        }
    }

    fun addRemoveFunds(stockId: Long, amount: Double) {
        viewModelScope.launch {
            val stocks = repository.getStocksByBucket(bucketId).first()
            val stock = stocks.find { it.stockId == stockId }
            if (stock != null) {
                val newValue = (stock.currentValue + amount).coerceAtLeast(0.0)
                repository.updateStock(stock.copy(currentValue = newValue))
            }
        }
    }

    fun updateTargetAllocation(stockId: Long, targetPercentage: Double) {
        viewModelScope.launch {
            val stocks = repository.getStocksByBucket(bucketId).first()
            val stock = stocks.find { it.stockId == stockId }
            if (stock != null) {
                repository.updateStock(stock.copy(targetPercentage = targetPercentage))
            }
        }
    }
}

class BucketDetailsViewModelFactory(
    private val repository: PortfolioRepository,
    private val bucketId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BucketDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BucketDetailsViewModel(repository, bucketId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
