package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprinttracker.data.Stock
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class StockUiEvent {
    data class Success(val message: String) : StockUiEvent()
    data class Error(val message: String) : StockUiEvent()
}

class StockViewModel(
    private val repository: PortfolioRepository,
    private val bucketId: Long? = null
) : ViewModel() {
    
    val stocks: Flow<List<Stock>> = if (bucketId != null) {
        repository.getStocksByBucket(bucketId)
    } else {
        repository.getAllStocks()
    }
    
    private val _uiEvent = MutableSharedFlow<StockUiEvent>()
    val uiEvent: SharedFlow<StockUiEvent> = _uiEvent.asSharedFlow()
    
    fun addStock(
        bucketId: Long,
        symbol: String,
        name: String,
        currentValue: Double,
        shares: Double? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val stock = Stock(
                bucketId = bucketId,
                symbol = symbol.uppercase(),
                name = name,
                currentValue = currentValue,
                shares = shares,
                notes = notes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.addStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock added successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to add stock"))
                }
            )
        }
    }
    
    fun updateStock(stock: Stock) {
        viewModelScope.launch {
            repository.updateStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock updated successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to update stock"))
                }
            )
        }
    }
    
    fun deleteStock(stock: Stock) {
        viewModelScope.launch {
            repository.deleteStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock deleted successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to delete stock"))
                }
            )
        }
    }
}
