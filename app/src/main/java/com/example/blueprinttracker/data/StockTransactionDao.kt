package com.example.blueprinttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockTransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: StockTransaction)

    @Query("SELECT * FROM stock_transactions WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getTransactionsBySymbol(symbol: String): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions WHERE stockId = :stockId ORDER BY timestamp DESC")
    fun getTransactionsByStockId(stockId: Long): Flow<List<StockTransaction>>
}
