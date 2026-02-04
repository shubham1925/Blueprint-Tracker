package com.example.blueprinttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketWithStocksDao {
    @Transaction
    @Query("SELECT * FROM buckets ORDER BY displayOrder ASC")
    fun getAllBucketsWithStocks(): Flow<List<BucketWithStocks>>
    
    @Transaction
    @Query("SELECT * FROM buckets WHERE bucketId = :bucketId")
    suspend fun getBucketWithStocks(bucketId: Long): BucketWithStocks?
}
