package com.example.blueprinttracker.di

import android.content.Context
import com.example.blueprinttracker.data.AppDatabase
import com.example.blueprinttracker.data.repository.PortfolioRepository

object ServiceLocator {
    private var database: AppDatabase? = null
    
    private fun provideDatabase(context: Context): AppDatabase {
        return database ?: AppDatabase.getDatabase(context).also { database = it }
    }
    
    fun providePortfolioRepository(context: Context): PortfolioRepository {
        val db = provideDatabase(context)
        return PortfolioRepository(
            bucketDao = db.bucketDao(),
            stockDao = db.stockDao(),
            bucketWithStocksDao = db.bucketWithStocksDao(),
            snapshotDao = db.portfolioSnapshotDao(),
            transactionDao = db.stockTransactionDao()
        )
    }
}
