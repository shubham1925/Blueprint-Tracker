package com.example.blueprinttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Stock::class,
            parentColumns = ["stockId"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stockId")]
)
data class StockTransaction(
    @PrimaryKey(autoGenerate = true)
    val transactionId: Long = 0,
    val stockId: Long,
    val symbol: String, // Keep symbol for history even if stock is deleted (though FK will delete it now)
    val amount: Double, // Positive for buy, negative for sell
    val timestamp: Long,
    val type: String // "BUY", "SELL", "ADD_FUNDS", "REMOVE_FUNDS"
)
