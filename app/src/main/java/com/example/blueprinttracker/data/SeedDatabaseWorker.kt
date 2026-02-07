package com.example.blueprinttracker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope

class SeedDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val bucketDao = database.bucketDao()
            val stockDao = database.stockDao()

            if (bucketDao.getCount() > 0) return@coroutineScope Result.success()

            val now = System.currentTimeMillis()
            val buckets = listOf(
                Bucket(bucketId = 1, name = "ETF", targetPercentage = 30.0, color = "#4285F4", displayOrder = 1, createdAt = now, updatedAt = now),
                Bucket(bucketId = 2, name = "DGIF", targetPercentage = 30.0, color = "#34A853", displayOrder = 2, createdAt = now, updatedAt = now),
                Bucket(bucketId = 3, name = "Growth", targetPercentage = 30.0, color = "#FBBC05", displayOrder = 3, createdAt = now, updatedAt = now),
                Bucket(bucketId = 4, name = "Spec", targetPercentage = 10.0, color = "#EA4335", displayOrder = 4, createdAt = now, updatedAt = now)
            )
            buckets.forEach { bucketDao.insertBucket(it) }

            val stocks = listOf(
                Stock(bucketId = 1, symbol = "VTI", name = "Vanguard Total Stock Market ETF", currentValue = 1000.0, targetPercentage = 50.0, createdAt = now, updatedAt = now),
                Stock(bucketId = 1, symbol = "VXUS", name = "Vanguard Total International Stock ETF", currentValue = 500.0, targetPercentage = 25.0, createdAt = now, updatedAt = now),
                Stock(bucketId = 2, symbol = "SCHD", name = "Schwab US Dividend Equity ETF", currentValue = 1200.0, targetPercentage = 60.0, createdAt = now, updatedAt = now),
                Stock(bucketId = 3, symbol = "GOOGL", name = "Alphabet Inc.", currentValue = 800.0, targetPercentage = 40.0, createdAt = now, updatedAt = now),
                Stock(bucketId = 4, symbol = "TSLA", name = "Tesla, Inc.", currentValue = 1500.0, targetPercentage = 50.0, createdAt = now, updatedAt = now)
            )
            stocks.forEach { stockDao.insertStock(it) }

            Result.success()
        } catch (ex: Exception) {
            Result.failure()
        }
    }
}
