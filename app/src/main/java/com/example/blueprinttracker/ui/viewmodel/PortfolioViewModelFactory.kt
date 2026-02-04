package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.blueprinttracker.data.repository.PortfolioRepository

class PortfolioViewModelFactory(
    private val repository: PortfolioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(BucketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BucketViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
