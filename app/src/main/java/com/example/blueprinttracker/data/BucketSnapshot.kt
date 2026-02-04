package com.example.blueprinttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bucket_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioSnapshot::class,
            parentColumns = ["snapshotId"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["bucketId"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snapshotId"), Index("bucketId")]
)
data class BucketSnapshot(
    @PrimaryKey(autoGenerate = true)
    val bucketSnapshotId: Long = 0,
    
    val snapshotId: Long,          // Foreign key to PortfolioSnapshot
    val bucketId: Long,            // Foreign key to Bucket
    val totalValue: Double,        // Bucket value at snapshot time
    val actualPercentage: Double,  // Actual allocation percentage
    val targetPercentage: Double   // Target at that time (for historical accuracy)
)
