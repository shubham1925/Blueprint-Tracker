package com.example.blueprinttracker.data.repository

import com.example.blueprinttracker.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class PortfolioRepository(
    private val bucketDao: BucketDao,
    private val stockDao: StockDao,
    private val bucketWithStocksDao: BucketWithStocksDao,
    private val snapshotDao: PortfolioSnapshotDao
) {
    // Bucket operations
    fun getAllBuckets(): Flow<List<Bucket>> = bucketDao.getAllBuckets()
    
    suspend fun getBucketById(bucketId: Long): Bucket? = bucketDao.getBucketById(bucketId)

    suspend fun addBucket(bucket: Bucket): Result<Long> {
        return try {
            val currentTotal = bucketDao.getTotalTargetPercentage() ?: 0.0
            if (currentTotal + bucket.targetPercentage > 100.0) {
                Result.failure(Exception("Total target percentage exceeds 100%"))
            } else {
                val id = bucketDao.insertBucket(bucket)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBucket(bucket: Bucket): Result<Unit> {
        return try {
            val currentTotal = bucketDao.getTotalTargetPercentage() ?: 0.0
            val oldBucket = bucketDao.getBucketById(bucket.bucketId)
            val adjustedTotal = currentTotal - (oldBucket?.targetPercentage ?: 0.0) + bucket.targetPercentage
            
            if (adjustedTotal > 100.0) {
                Result.failure(Exception("Total target percentage exceeds 100%"))
            } else {
                bucketDao.updateBucket(bucket.copy(updatedAt = System.currentTimeMillis()))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBucket(bucket: Bucket): Result<Unit> {
        return try {
            bucketDao.deleteBucket(bucket)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Stock operations
    fun getStocksByBucket(bucketId: Long): Flow<List<Stock>> = 
        stockDao.getStocksByBucket(bucketId)
    
    fun getAllStocks(): Flow<List<Stock>> = stockDao.getAllStocks()
    
    suspend fun addStock(stock: Stock): Result<Long> {
        return try {
            val id = stockDao.insertStock(stock)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateStock(stock: Stock): Result<Unit> {
        return try {
            stockDao.updateStock(stock.copy(updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteStock(stock: Stock): Result<Unit> {
        return try {
            stockDao.deleteStock(stock)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Portfolio summary
    fun getPortfolioSummary(): Flow<PortfolioSummary> {
        return bucketWithStocksDao.getAllBucketsWithStocks()
            .map { bucketsWithStocks ->
                val totalPortfolioValue = bucketsWithStocks.sumOf { bws ->
                    bws.stocks.sumOf { it.currentValue }
                }
                
                val bucketAllocations = bucketsWithStocks.map { bws ->
                    val bucketValue = bws.stocks.sumOf { it.currentValue }
                    val currentPercentage = if (totalPortfolioValue > 0) {
                        (bucketValue / totalPortfolioValue) * 100.0
                    } else {
                        0.0
                    }
                    
                    BucketAllocation(
                        bucket = bws.bucket,
                        currentValue = bucketValue,
                        currentPercentage = currentPercentage,
                        targetPercentage = bws.bucket.targetPercentage,
                        difference = currentPercentage - bws.bucket.targetPercentage,
                        stockCount = bws.stocks.size
                    )
                }
                
                PortfolioSummary(
                    totalValue = totalPortfolioValue,
                    buckets = bucketAllocations
                )
            }
    }

    fun getBucketDetailSummary(bucketId: Long): Flow<BucketDetailSummary?> {
        val bucketFlow = bucketDao.getAllBuckets().map { it.find { b -> b.bucketId == bucketId } }
        val stocksFlow = stockDao.getStocksByBucket(bucketId)

        return combine(bucketFlow, stocksFlow) { bucket, stocks ->
            if (bucket == null) return@combine null

            val totalBucketValue = stocks.sumOf { it.currentValue }
            val stockDetails = stocks.map { stock ->
                val currentPercentage = if (totalBucketValue > 0) {
                    (stock.currentValue / totalBucketValue) * 100.0
                } else {
                    0.0
                }
                StockDetail(
                    stock = stock,
                    currentPercentage = currentPercentage,
                    isOverAllocated = currentPercentage > stock.targetPercentage
                )
            }

            BucketDetailSummary(
                bucket = bucket,
                stocks = stockDetails,
                totalBucketValue = totalBucketValue
            )
        }
    }
    
    // Snapshot operations
    suspend fun createSnapshot(note: String? = null): Result<Unit> {
        return try {
            val totalValue = stockDao.getTotalPortfolioValue() ?: 0.0
            val snapshot = PortfolioSnapshot(
                timestamp = System.currentTimeMillis(),
                totalValue = totalValue,
                notes = note
            )
            val snapshotId = snapshotDao.insertSnapshot(snapshot)
            
            // Create bucket snapshots
            val buckets = bucketDao.getAllBuckets().first()
            val bucketSnapshots = buckets.map { bucket ->
                val bucketValue = stockDao.getTotalValueForBucket(bucket.bucketId) ?: 0.0
                val actualPercentage = if (totalValue > 0) {
                    (bucketValue / totalValue) * 100.0
                } else {
                    0.0
                }
                
                BucketSnapshot(
                    snapshotId = snapshotId,
                    bucketId = bucket.bucketId,
                    totalValue = bucketValue,
                    actualPercentage = actualPercentage,
                    targetPercentage = bucket.targetPercentage
                )
            }
            
            snapshotDao.insertBucketSnapshots(bucketSnapshots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getHistoricalData(daysBack: Int = 30): List<HistoricalDataPoint> {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        val allocations = snapshotDao.getHistoricalAllocations(startTime)
        
        // Group by timestamp
        return allocations.groupBy { it.timestamp }
            .map { (timestamp, records) ->
                HistoricalDataPoint(
                    timestamp = timestamp,
                    bucketAllocations = records.associate { 
                        it.bucketId to it.actualPercentage 
                    }
                )
            }
            .sortedBy { it.timestamp }
    }
}
