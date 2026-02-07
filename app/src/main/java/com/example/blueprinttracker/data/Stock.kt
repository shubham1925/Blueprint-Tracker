package com.example.blueprinttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stocks",
    foreignKeys = [
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["bucketId"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bucketId")]
)
data class Stock(
    @PrimaryKey(autoGenerate = true)
    val stockId: Long = 0,
    
    val bucketId: Long,            // Foreign key to Bucket
    val symbol: String,            // Stock ticker symbol (e.g., "AAPL")
    val name: String,              // Company/fund name
    val currentValue: Double,      // Current value in portfolio
    val targetPercentage: Double = 0.0, // Target allocation within the bucket (0.0 - 100.0)
    val shares: Double? = null,    // Optional: number of shares held
    val notes: String? = null,     // Optional user notes
    val createdAt: Long,
    val updatedAt: Long
)
