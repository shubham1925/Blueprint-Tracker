package com.example.blueprinttracker.data

data class HistoricalAllocationData(
    val timestamp: Long,
    val bucketId: Long,
    val actualPercentage: Double
)
