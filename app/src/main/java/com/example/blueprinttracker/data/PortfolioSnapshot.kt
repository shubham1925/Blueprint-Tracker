package com.example.blueprinttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true)
    val snapshotId: Long = 0,
    
    val timestamp: Long,           // When snapshot was taken
    val totalValue: Double,        // Total portfolio value at this time
    val notes: String? = null      // Optional notes for this snapshot
)
