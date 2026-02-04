# Portfolio Tracker Android App - Design Document

## 1. Overview

### 1.1 Purpose
A portfolio tracking application that allows users to manage investment portfolios with predefined allocation targets, track individual stocks across different buckets, and monitor portfolio composition over time.

### 1.2 Key Features
- Define portfolio buckets with target allocation percentages
- Add/remove stocks and assign them to buckets
- Update stock balances/values
- View actual vs target allocation for each bucket
- Historical view of portfolio allocation changes over time

---

## 2. Architecture: MVVM Pattern

### 2.1 Architecture Layers

```
┌─────────────────────────────────────┐
│         View (UI Layer)             │
│  - Activities                       │
│  - Fragments                        │
│  - XML Layouts                      │
│  - Adapters                         │
└──────────────┬──────────────────────┘
               │
               │ observes LiveData/StateFlow
               │ sends user actions
               ▼
┌─────────────────────────────────────┐
│      ViewModel Layer                │
│  - Holds UI State                   │
│  - Business Logic                   │
│  - Exposes LiveData/StateFlow       │
└──────────────┬──────────────────────┘
               │
               │ calls repository methods
               │
               ▼
┌─────────────────────────────────────┐
│      Repository Layer               │
│  - Single source of truth           │
│  - Data coordination                │
│  - Business rules enforcement       │
└──────────────┬──────────────────────┘
               │
               │ uses
               │
               ▼
┌─────────────────────────────────────┐
│      Data Layer                     │
│  - Room Database (Local)            │
│  - DAOs                             │
│  - Entity Models                    │
└─────────────────────────────────────┘
```

---

## 3. Data Model

### 3.1 Database Entities

#### Bucket Entity
```kotlin
@Entity(tableName = "buckets")
data class Bucket(
    @PrimaryKey(autoGenerate = true)
    val bucketId: Long = 0,
    
    val name: String,              // e.g., "US Stocks", "Bonds", "International"
    val targetPercentage: Double,  // Target allocation (0.0 - 100.0)
    val color: String,             // Hex color for UI visualization
    val displayOrder: Int,         // Order in which to display
    val createdAt: Long,           // Timestamp
    val updatedAt: Long            // Timestamp
)
```

#### Stock Entity
```kotlin
@Entity(
    tableName = "stocks",
    foreignKeys = [
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["bucketId"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bucketId")]
)
data class Stock(
    @PrimaryKey(autoGenerate = true)
    val stockId: Long = 0,
    
    val bucketId: Long,            // Foreign key to Bucket
    val symbol: String,            // Stock ticker symbol (e.g., "AAPL")
    val name: String,              // Company/fund name
    val currentValue: Double,      // Current value in portfolio
    val shares: Double? = null,    // Optional: number of shares held
    val notes: String? = null,     // Optional user notes
    val createdAt: Long,
    val updatedAt: Long
)
```

#### PortfolioSnapshot Entity
```kotlin
@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true)
    val snapshotId: Long = 0,
    
    val timestamp: Long,           // When snapshot was taken
    val totalValue: Double,        // Total portfolio value at this time
    val notes: String? = null      // Optional notes for this snapshot
)
```

#### BucketSnapshot Entity
```kotlin
@Entity(
    tableName = "bucket_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioSnapshot::class,
            parentColumns = ["snapshotId"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["bucketId"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snapshotId"), Index("bucketId")]
)
data class BucketSnapshot(
    @PrimaryKey(autoGenerate = true)
    val bucketSnapshotId: Long = 0,
    
    val snapshotId: Long,          // Foreign key to PortfolioSnapshot
    val bucketId: Long,            // Foreign key to Bucket
    val totalValue: Double,        // Bucket value at snapshot time
    val actualPercentage: Double,  // Actual allocation percentage
    val targetPercentage: Double   // Target at that time (for historical accuracy)
)
```

### 3.2 Data Transfer Objects (DTOs)

#### BucketWithStocks
```kotlin
data class BucketWithStocks(
    @Embedded val bucket: Bucket,
    @Relation(
        parentColumn = "bucketId",
        entityColumn = "bucketId"
    )
    val stocks: List<Stock>
)
```

#### PortfolioSummary
```kotlin
data class PortfolioSummary(
    val totalValue: Double,
    val buckets: List<BucketAllocation>
)

data class BucketAllocation(
    val bucket: Bucket,
    val currentValue: Double,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val difference: Double,          // currentPercentage - targetPercentage
    val stockCount: Int
)
```

#### HistoricalDataPoint
```kotlin
data class HistoricalDataPoint(
    val timestamp: Long,
    val bucketAllocations: Map<Long, Double>  // bucketId to percentage
)
```

---

## 4. Data Access Layer (DAOs)

### 4.1 BucketDao
```kotlin
@Dao
interface BucketDao {
    @Query("SELECT * FROM buckets ORDER BY displayOrder ASC")
    fun getAllBuckets(): Flow<List<Bucket>>
    
    @Query("SELECT * FROM buckets WHERE bucketId = :bucketId")
    suspend fun getBucketById(bucketId: Long): Bucket?
    
    @Insert
    suspend fun insertBucket(bucket: Bucket): Long
    
    @Update
    suspend fun updateBucket(bucket: Bucket)
    
    @Delete
    suspend fun deleteBucket(bucket: Bucket)
    
    @Query("SELECT SUM(targetPercentage) FROM buckets")
    suspend fun getTotalTargetPercentage(): Double?
}
```

### 4.2 StockDao
```kotlin
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
```

### 4.3 PortfolioSnapshotDao
```kotlin
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

data class HistoricalAllocationData(
    val timestamp: Long,
    val bucketId: Long,
    val actualPercentage: Double
)
```

### 4.4 BucketWithStocksDao
```kotlin
@Dao
interface BucketWithStocksDao {
    @Transaction
    @Query("SELECT * FROM buckets ORDER BY displayOrder ASC")
    fun getAllBucketsWithStocks(): Flow<List<BucketWithStocks>>
    
    @Transaction
    @Query("SELECT * FROM buckets WHERE bucketId = :bucketId")
    suspend fun getBucketWithStocks(bucketId: Long): BucketWithStocks?
}
```

---

## 5. Repository Layer

### 5.1 PortfolioRepository
```kotlin
class PortfolioRepository(
    private val bucketDao: BucketDao,
    private val stockDao: StockDao,
    private val bucketWithStocksDao: BucketWithStocksDao,
    private val snapshotDao: PortfolioSnapshotDao
) {
    // Bucket operations
    fun getAllBuckets(): Flow<List<Bucket>> = bucketDao.getAllBuckets()
    
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
```

---

## 6. ViewModel Layer

### 6.1 PortfolioViewModel
```kotlin
class PortfolioViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()
    
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()
    
    init {
        loadPortfolioSummary()
    }
    
    private fun loadPortfolioSummary() {
        viewModelScope.launch {
            repository.getPortfolioSummary()
                .catch { e ->
                    _uiState.value = PortfolioUiState.Error(e.message ?: "Unknown error")
                }
                .collect { summary ->
                    _uiState.value = PortfolioUiState.Success(summary)
                }
        }
    }
    
    fun createSnapshot(note: String? = null) {
        viewModelScope.launch {
            repository.createSnapshot(note).fold(
                onSuccess = {
                    _snackbarMessage.emit("Snapshot created successfully")
                },
                onFailure = { e ->
                    _snackbarMessage.emit("Failed to create snapshot: ${e.message}")
                }
            )
        }
    }
    
    fun refreshData() {
        loadPortfolioSummary()
    }
}

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(val summary: PortfolioSummary) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}
```

### 6.2 BucketViewModel
```kotlin
class BucketViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    val allBuckets: Flow<List<Bucket>> = repository.getAllBuckets()
    
    private val _uiEvent = MutableSharedFlow<BucketUiEvent>()
    val uiEvent: SharedFlow<BucketUiEvent> = _uiEvent.asSharedFlow()
    
    fun addBucket(name: String, targetPercentage: Double, color: String) {
        viewModelScope.launch {
            val bucket = Bucket(
                name = name,
                targetPercentage = targetPercentage,
                color = color,
                displayOrder = 0, // Will be adjusted
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.addBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket added successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to add bucket"))
                }
            )
        }
    }
    
    fun updateBucket(bucket: Bucket) {
        viewModelScope.launch {
            repository.updateBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket updated successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to update bucket"))
                }
            )
        }
    }
    
    fun deleteBucket(bucket: Bucket) {
        viewModelScope.launch {
            repository.deleteBucket(bucket).fold(
                onSuccess = {
                    _uiEvent.emit(BucketUiEvent.Success("Bucket deleted successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(BucketUiEvent.Error(e.message ?: "Failed to delete bucket"))
                }
            )
        }
    }
}

sealed class BucketUiEvent {
    data class Success(val message: String) : BucketUiEvent()
    data class Error(val message: String) : BucketUiEvent()
}
```

### 6.3 StockViewModel
```kotlin
class StockViewModel(
    private val repository: PortfolioRepository,
    private val bucketId: Long? = null
) : ViewModel() {
    
    val stocks: Flow<List<Stock>> = if (bucketId != null) {
        repository.getStocksByBucket(bucketId)
    } else {
        repository.getAllStocks()
    }
    
    private val _uiEvent = MutableSharedFlow<StockUiEvent>()
    val uiEvent: SharedFlow<StockUiEvent> = _uiEvent.asSharedFlow()
    
    fun addStock(
        bucketId: Long,
        symbol: String,
        name: String,
        currentValue: Double,
        shares: Double? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val stock = Stock(
                bucketId = bucketId,
                symbol = symbol.uppercase(),
                name = name,
                currentValue = currentValue,
                shares = shares,
                notes = notes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.addStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock added successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to add stock"))
                }
            )
        }
    }
    
    fun updateStock(stock: Stock) {
        viewModelScope.launch {
            repository.updateStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock updated successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to update stock"))
                }
            )
        }
    }
    
    fun deleteStock(stock: Stock) {
        viewModelScope.launch {
            repository.deleteStock(stock).fold(
                onSuccess = {
                    _uiEvent.emit(StockUiEvent.Success("Stock deleted successfully"))
                },
                onFailure = { e ->
                    _uiEvent.emit(StockUiEvent.Error(e.message ?: "Failed to delete stock"))
                }
            )
        }
    }
}

sealed class StockUiEvent {
    data class Success(val message: String) : StockUiEvent()
    data class Error(val message: String) : StockUiEvent()
}
```

### 6.4 HistoryViewModel
```kotlin
class HistoryViewModel(
    private val repository: PortfolioRepository
) : ViewModel() {
    
    private val _historicalData = MutableStateFlow<List<HistoricalDataPoint>>(emptyList())
    val historicalData: StateFlow<List<HistoricalDataPoint>> = _historicalData.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.THIRTY_DAYS)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    init {
        loadHistoricalData()
    }
    
    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        loadHistoricalData()
    }
    
    private fun loadHistoricalData() {
        viewModelScope.launch {
            val days = when (_selectedTimeRange.value) {
                TimeRange.SEVEN_DAYS -> 7
                TimeRange.THIRTY_DAYS -> 30
                TimeRange.NINETY_DAYS -> 90
                TimeRange.ONE_YEAR -> 365
            }
            
            val data = repository.getHistoricalData(days)
            _historicalData.value = data
        }
    }
}

enum class TimeRange {
    SEVEN_DAYS,
    THIRTY_DAYS,
    NINETY_DAYS,
    ONE_YEAR
}
```

---

## 7. View Layer (UI)

### 7.1 Main Navigation Structure

```
MainActivity
├── PortfolioOverviewFragment (Home)
├── BucketsFragment (Manage buckets)
├── StocksFragment (View all stocks)
└── HistoryFragment (Historical charts)
```

### 7.2 Screen Specifications

#### 7.2.1 PortfolioOverviewFragment
**Purpose:** Display current portfolio summary with actual vs target allocations

**UI Components:**
- Total portfolio value header
- RecyclerView of buckets showing:
  - Bucket name and color indicator
  - Current value and percentage
  - Target percentage
  - Visual progress bar (actual vs target)
  - Difference indicator (±X%)
- FloatingActionButton to create snapshot
- Pull-to-refresh functionality

**ViewModel:** PortfolioViewModel

#### 7.2.2 BucketsFragment
**Purpose:** Manage portfolio buckets

**UI Components:**
- RecyclerView of buckets with:
  - Bucket details
  - Edit and delete actions
- FloatingActionButton to add new bucket
- Dialog for add/edit bucket:
  - Name input
  - Target percentage input (with validation)
  - Color picker
  - Save/Cancel buttons

**ViewModel:** BucketViewModel

#### 7.2.3 BucketDetailFragment
**Purpose:** View and manage stocks within a specific bucket

**UI Components:**
- Bucket summary header
- RecyclerView of stocks showing:
  - Symbol and name
  - Current value
  - Number of shares (if applicable)
  - Edit and delete actions
- FloatingActionButton to add stock to this bucket

**ViewModel:** StockViewModel (with bucketId)

#### 7.2.4 StocksFragment
**Purpose:** View all stocks across all buckets

**UI Components:**
- Search/filter bar
- RecyclerView of all stocks with grouping by bucket
- Each stock item shows:
  - Symbol, name, bucket
  - Current value
  - Edit/delete actions

**ViewModel:** StockViewModel

#### 7.2.5 AddEditStockFragment
**Purpose:** Add or edit a stock

**UI Components:**
- Bucket selection spinner (if adding new)
- Symbol input
- Name input
- Current value input
- Shares input (optional)
- Notes input (optional)
- Save/Cancel buttons

**ViewModel:** StockViewModel

#### 7.2.6 HistoryFragment
**Purpose:** Display historical allocation changes

**UI Components:**
- Time range selector (7d, 30d, 90d, 1y)
- Line chart showing allocation percentages over time
- Legend with bucket colors
- Optional: Data table view toggle

**ViewModel:** HistoryViewModel
**Charting Library:** MPAndroidChart or similar

---

## 8. Database Setup

### 8.1 AppDatabase
```kotlin
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
```

---

## 9. Dependency Injection

### 9.1 Manual DI (Simple Approach)
```kotlin
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
            snapshotDao = db.portfolioSnapshotDao()
        )
    }
}

// ViewModelFactory
class PortfolioViewModelFactory(
    private val repository: PortfolioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### 9.2 Hilt DI (Recommended for larger apps)
```kotlin
// Module
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideBucketDao(database: AppDatabase): BucketDao {
        return database.bucketDao()
    }
    
    @Provides
    fun provideStockDao(database: AppDatabase): StockDao {
        return database.stockDao()
    }
    
    // ... other DAO providers
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providePortfolioRepository(
        bucketDao: BucketDao,
        stockDao: StockDao,
        bucketWithStocksDao: BucketWithStocksDao,
        snapshotDao: PortfolioSnapshotDao
    ): PortfolioRepository {
        return PortfolioRepository(bucketDao, stockDao, bucketWithStocksDao, snapshotDao)
    }
}

// ViewModel with Hilt
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository
) : ViewModel() {
    // ... implementation
}
```

---

## 10. Key Features Implementation

### 10.1 Automatic Snapshot Creation
Consider creating snapshots automatically when significant changes occur:

```kotlin
// In Repository
suspend fun updateStockWithSnapshot(stock: Stock): Result<Unit> {
    return try {
        updateStock(stock)
        // Create snapshot after significant value changes
        if (shouldCreateSnapshot()) {
            createSnapshot("Auto-snapshot after stock update")
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun shouldCreateSnapshot(): Boolean {
    // Logic to determine if snapshot should be created
    // E.g., once per day, or after X% portfolio change
    return true
}
```

### 10.2 Rebalancing Suggestions
```kotlin
data class RebalancingSuggestion(
    val fromBucket: Bucket,
    val toBucket: Bucket,
    val amount: Double,
    val reason: String
)

suspend fun getRebalancingSuggestions(): List<RebalancingSuggestion> {
    val summary = getPortfolioSummary().first()
    val suggestions = mutableListOf<RebalancingSuggestion>()
    
    // Find buckets that are over-allocated
    val overAllocated = summary.buckets.filter { it.difference > 1.0 }
    val underAllocated = summary.buckets.filter { it.difference < -1.0 }
    
    // Generate rebalancing suggestions
    for (over in overAllocated) {
        for (under in underAllocated) {
            val moveAmount = minOf(
                over.currentValue * (over.difference / 100.0),
                under.currentValue * (abs(under.difference) / 100.0)
            )
            
            if (moveAmount > 0) {
                suggestions.add(
                    RebalancingSuggestion(
                        fromBucket = over.bucket,
                        toBucket = under.bucket,
                        amount = moveAmount,
                        reason = "Move $${"%.2f".format(moveAmount)} to reach target allocation"
                    )
                )
            }
        }
    }
    
    return suggestions
}
```

---

## 11. Testing Strategy

### 11.1 Unit Tests
- Test ViewModels with fake repositories
- Test Repository business logic
- Test data validation

### 11.2 Integration Tests
- Test DAOs with in-memory database
- Test Repository with real database

### 11.3 UI Tests
- Test navigation flows
- Test data entry forms
- Test list interactions

---

## 12. Future Enhancements

### Phase 2 Features:
1. Import stock prices from external API
2. Performance metrics (gains/losses)
3. Dividend tracking
4. Tax lot management
5. Export to CSV/PDF
6. Backup and restore
7. Multiple portfolio support
8. Notifications for rebalancing opportunities
9. Dark mode support
10. Widget for home screen

### Phase 3 Features:
1. Cloud sync
2. Share portfolio with financial advisor
3. Integration with brokerage APIs
4. Advanced charts and analytics
5. Goal-based planning

---

## 13. Technology Stack Summary

**Language:** Kotlin
**Architecture:** MVVM
**Database:** Room (SQLite)
**Async:** Coroutines + Flow
**DI:** Hilt (recommended) or manual
**UI:** Jetpack Compose or XML layouts
**Navigation:** Navigation Component
**Charts:** MPAndroidChart
**Testing:** JUnit, Mockito, Espresso

---

## 14. Getting Started Checklist

1. ✓ Set up new Android Studio project
2. ✓ Add dependencies (Room, Coroutines, ViewModel, etc.)
3. ✓ Create database entities
4. ✓ Implement DAOs
5. ✓ Build Repository layer
6. ✓ Create ViewModels
7. ✓ Design UI layouts
8. ✓ Implement navigation
9. ✓ Connect ViewModels to UI
10. ✓ Add data validation
11. ✓ Implement charting
12. ✓ Test thoroughly
13. ✓ Polish UI/UX
14. ✓ Optimize performance

---

## 15. Development Timeline Estimate

**Week 1-2:** Database setup, entities, DAOs, repository
**Week 3-4:** ViewModels and basic UI screens
**Week 5:** Stock and bucket CRUD operations
**Week 6:** Portfolio summary and allocation views
**Week 7:** Historical tracking and charts
**Week 8:** Testing, bug fixes, polish

Total: ~8 weeks for MVP (assuming part-time development)

---

This design document provides a complete blueprint for building your portfolio tracker app using MVVM architecture. Start with the data layer (database entities and DAOs), then build up to the repository, ViewModels, and finally the UI layer.
