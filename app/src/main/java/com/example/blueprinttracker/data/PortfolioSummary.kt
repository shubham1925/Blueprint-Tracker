package com.example.blueprinttracker.data

data class PortfolioSummary(
    val totalValue: Double,
    val buckets: List<BucketAllocation>
)

data class BucketAllocation(
    val bucket: Bucket,
    val currentValue: Double,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val difference: Double,          // currentPercentage - targetPercentage
    val stockCount: Int
)
