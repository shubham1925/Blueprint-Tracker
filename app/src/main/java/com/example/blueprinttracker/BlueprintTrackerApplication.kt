package com.example.blueprinttracker

import android.app.Application
import com.example.blueprinttracker.data.AppDatabase
import com.example.blueprinttracker.data.repository.PortfolioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class BlueprintTrackerApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: PortfolioRepository by lazy { 
        PortfolioRepository(
            database.bucketDao(),
            database.stockDao(),
            database.bucketWithStocksDao(),
            database.portfolioSnapshotDao()
        )
    }
}
