package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprinttracker.data.Bucket
import com.example.blueprinttracker.data.PortfolioSummary
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(val summary: PortfolioSummary) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}

class PortfolioViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()
    
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()
    
    init {
        loadPortfolioSummary()
    }
    
    private fun loadPortfolioSummary() {
        viewModelScope.launch {
            repository.getPortfolioSummary()
                .catch { e ->
                    _uiState.value = PortfolioUiState.Error(e.message ?: "Unknown error")
                }
                .collect { summary ->
                    _uiState.value = PortfolioUiState.Success(summary)
                }
        }
    }
    
    fun createSnapshot(note: String? = null) {
        viewModelScope.launch {
            repository.createSnapshot(note).fold(
                onSuccess = {
                    _snackbarMessage.emit("Snapshot created successfully")
                },
                onFailure = { e ->
                    _snackbarMessage.emit("Failed to create snapshot: ${e.message}")
                }
            )
        }
    }

    fun updateAllBucketAllocations(buckets: List<Bucket>) {
        viewModelScope.launch {
            repository.updateBuckets(buckets).fold(
                onSuccess = {
                    _snackbarMessage.emit("Buckets updated successfully")
                },
                onFailure = { e ->
                    _snackbarMessage.emit("Failed to update buckets: ${e.message}")
                }
            )
        }
    }

    fun updateBucketAllocation(bucket: Bucket, newTarget: Double) {
        viewModelScope.launch {
            repository.updateBucket(bucket.copy(targetPercentage = newTarget)).fold(
                onSuccess = {
                    _snackbarMessage.emit("Bucket updated successfully")
                },
                onFailure = { e ->
                    _snackbarMessage.emit("Failed to update bucket: ${e.message}")
                }
            )
        }
    }
    
    fun refreshData() {
        loadPortfolioSummary()
    }
}
