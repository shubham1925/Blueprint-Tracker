package com.example.blueprinttracker.data

data class StockDetail(
    val stock: Stock,
    val currentPercentage: Double,
    val isOverAllocated: Boolean
)

data class BucketDetailSummary(
    val bucket: Bucket,
    val stocks: List<StockDetail>,
    val totalBucketValue: Double
)
