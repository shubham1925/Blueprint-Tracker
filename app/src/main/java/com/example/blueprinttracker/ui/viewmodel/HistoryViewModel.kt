package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprinttracker.data.HistoricalDataPoint
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimeRange {
    SEVEN_DAYS,
    THIRTY_DAYS,
    NINETY_DAYS,
    ONE_YEAR
}

class HistoryViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    private val _historicalData = MutableStateFlow<List<HistoricalDataPoint>>(emptyList())
    val historicalData: StateFlow<List<HistoricalDataPoint>> = _historicalData.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.THIRTY_DAYS)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    init {
        loadHistoricalData()
    }
    
    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        loadHistoricalData()
    }
    
    private fun loadHistoricalData() {
        viewModelScope.launch {
            val days = when (_selectedTimeRange.value) {
                TimeRange.SEVEN_DAYS -> 7
                TimeRange.THIRTY_DAYS -> 30
                TimeRange.NINETY_DAYS -> 90
                TimeRange.ONE_YEAR -> 365
            }
            
            val data = repository.getHistoricalData(days)
            _historicalData.value = data
        }
    }
}
