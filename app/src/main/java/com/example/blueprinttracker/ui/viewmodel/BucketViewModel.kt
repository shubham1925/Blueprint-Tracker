package com.example.blueprinttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprinttracker.data.Bucket
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class BucketUiEvent {
    data class Success(val message: String) : BucketUiEvent()
    data class Error(val message: String) : BucketUiEvent()
}

class BucketViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    val allBuckets: Flow<List<Bucket>> = repository.getAllBuckets()
    
    private val _uiEvent = MutableSharedFlow<BucketUiEvent>()
    val uiEvent: SharedFlow<BucketUiEvent> = _uiEvent.asSharedFlow()
    
    fun addBucket(name: String, targetPercentage: Double, color: String) {
        viewModelScope.launch {
            val bucket = Bucket(
                name = name,
                targetPercentage = targetPercentage,
                color = color,
                displayOrder = 0, // Will be adjusted
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.addBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket added successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to add bucket"))
                }
            )
        }
    }
    
    fun updateBucket(bucket: Bucket) {
        viewModelScope.launch {
            repository.updateBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket updated successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to update bucket"))
                }
            )
        }
    }
    
    fun deleteBucket(bucket: Bucket) {
        viewModelScope.launch {
            repository.deleteBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket deleted successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to delete bucket"))
                }
            )
        }
    }
}
