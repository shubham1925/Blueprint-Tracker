package com.example.blueprinttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buckets")
data class Bucket(
    @PrimaryKey(autoGenerate = true)
    val bucketId: Long = 0,
    
    val name: String,              // e.g., "US Stocks", "Bonds", "International"
    val targetPercentage: Double,  // Target allocation (0.0 - 100.0)
    val color: String,             // Hex color for UI visualization
    val displayOrder: Int,         // Order in which to display
    val createdAt: Long,           // Timestamp
    val updatedAt: Long            // Timestamp
)
