package com.example.blueprinttracker.data

data class HistoricalDataPoint(
    val timestamp: Long,
    val bucketAllocations: Map<Long, Double>  // bucketId to percentage
)
