package com.example.blueprinttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks WHERE bucketId = :bucketId ORDER BY symbol ASC")
    fun getStocksByBucket(bucketId: Long): Flow<List<Stock>>
    
    @Query("SELECT * FROM stocks ORDER BY symbol ASC")
    fun getAllStocks(): Flow<List<Stock>>
    
    @Query("SELECT * FROM stocks WHERE stockId = :stockId")
    suspend fun getStockById(stockId: Long): Stock?
    
    @Insert
    suspend fun insertStock(stock: Stock): Long
    
    @Update
    suspend fun updateStock(stock: Stock)
    
    @Delete
    suspend fun deleteStock(stock: Stock)
    
    @Query("SELECT SUM(currentValue) FROM stocks WHERE bucketId = :bucketId")
    suspend fun getTotalValueForBucket(bucketId: Long): Double?
    
    @Query("SELECT SUM(currentValue) FROM stocks")
    suspend fun getTotalPortfolioValue(): Double?
}
