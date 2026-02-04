package com.example.blueprinttracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Bucket::class,
        Stock::class,
        PortfolioSnapshot::class,
        BucketSnapshot::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bucketDao(): BucketDao
    abstract fun stockDao(): StockDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun bucketWithStocksDao(): BucketWithStocksDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portfolio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
