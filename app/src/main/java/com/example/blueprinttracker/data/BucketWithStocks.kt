package com.example.blueprinttracker.data

import androidx.room.Embedded
import androidx.room.Relation

data class BucketWithStocks(
    @Embedded val bucket: Bucket,
    @Relation(
        parentColumn = "bucketId",
        entityColumn = "bucketId"
    )
    val stocks: List<Stock>
)
