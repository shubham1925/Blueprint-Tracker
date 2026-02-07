package com.example.blueprinttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {
    @Query("SELECT * FROM buckets ORDER BY displayOrder ASC")
    fun getAllBuckets(): Flow<List<Bucket>>
    
    @Query("SELECT * FROM buckets WHERE bucketId = :bucketId")
    suspend fun getBucketById(bucketId: Long): Bucket?
    
    @Insert
    suspend fun insertBucket(bucket: Bucket): Long
    
    @Update
    suspend fun updateBucket(bucket: Bucket)
    
    @Delete
    suspend fun deleteBucket(bucket: Bucket)
    
    @Query("SELECT SUM(targetPercentage) FROM buckets")
    suspend fun getTotalTargetPercentage(): Double?

    @Query("SELECT COUNT(*) FROM buckets")
    suspend fun getCount(): Int
}
