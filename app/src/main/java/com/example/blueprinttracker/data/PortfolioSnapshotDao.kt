package com.example.blueprinttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {
    @Insert
    suspend fun insertSnapshot(snapshot: PortfolioSnapshot): Long
    
    @Insert
    suspend fun insertBucketSnapshots(snapshots: List<BucketSnapshot>)
    
    @Query("""
        SELECT ps.*, bs.* FROM portfolio_snapshots ps
        LEFT JOIN bucket_snapshots bs ON ps.snapshotId = bs.snapshotId
        ORDER BY ps.timestamp DESC
        LIMIT :limit
    """)
    fun getRecentSnapshots(limit: Int = 30): Flow<Map<PortfolioSnapshot, List<BucketSnapshot>>>
    
    @Query("""
        SELECT ps.timestamp, bs.bucketId, bs.actualPercentage
        FROM portfolio_snapshots ps
        JOIN bucket_snapshots bs ON ps.snapshotId = bs.snapshotId
        WHERE ps.timestamp >= :startTime
        ORDER BY ps.timestamp ASC
    """)
    suspend fun getHistoricalAllocations(startTime: Long): List<HistoricalAllocationData>
    
    @Query("DELETE FROM portfolio_snapshots WHERE timestamp < :beforeTimestamp")
    suspend fun deleteSnapshotsBefore(beforeTimestamp: Long)
}
