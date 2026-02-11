package com.example.blueprinttracker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class SeedDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val bucketDao = database.bucketDao()

            if (bucketDao.getCount() > 0) {
                // Check if DGIF needs renaming in existing database
                val dgifBucket = bucketDao.getAllBuckets().first().find { it.name == "DGIF" }
                if (dgifBucket != null) {
                    bucketDao.updateBucket(dgifBucket.copy(name = "Dividend Growth", updatedAt = System.currentTimeMillis()))
                }
                return@coroutineScope Result.success()
            }

            val now = System.currentTimeMillis()
            val buckets = listOf(
                Bucket(bucketId = 1, name = "ETF", targetPercentage = 30.0, color = "#4285F4", displayOrder = 1, createdAt = now, updatedAt = now),
                Bucket(bucketId = 2, name = "Dividend Growth", targetPercentage = 30.0, color = "#34A853", displayOrder = 2, createdAt = now, updatedAt = now),
                Bucket(bucketId = 3, name = "Growth", targetPercentage = 30.0, color = "#FBBC05", displayOrder = 3, createdAt = now, updatedAt = now),
                Bucket(bucketId = 4, name = "Spec", targetPercentage = 10.0, color = "#EA4335", displayOrder = 4, createdAt = now, updatedAt = now)
            )
            buckets.forEach { bucketDao.insertBucket(it) }

            // Stocks are no longer seeded to provide a clean slate for new users.

            Result.success()
        } catch (ex: Exception) {
            Result.failure()
        }
    }
}
