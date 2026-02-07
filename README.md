# Blueprint Tracker

Blueprint Tracker is an Android application designed to help users manage their investment portfolios by organizing stocks into "buckets" with specific target allocations. It allows for tracking current values, managing target percentages, and performing simple buy/sell operations.

## Project Structure

The project follows a standard Android multi-module structure (currently with a single `:app` module) and uses the MVVM (Model-View-ViewModel) architectural pattern.

- **`:app`**: The main application module.
    - **`com.example.blueprinttracker.data`**: Contains the data layer, including Room entities (Stock, Bucket, PortfolioSnapshot), DAOs, and the database configuration.
    - **`com.example.blueprinttracker.data.repository`**: Contains the `PortfolioRepository`, which abstracts data access for the rest of the app.
    - **`com.example.blueprinttracker.ui`**: Contains UI-related components.
        - **`adapter`**: RecyclerView adapters (e.g., `StockAdapter`, `BucketAdapter`).
        - **`viewmodel`**: ViewModels that manage UI state and interact with the repository.
    - **`com.example.blueprinttracker`**: Contains Fragments and Activities.

## Android Version Requirements

- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## Key Dependencies

The project uses several modern Android libraries:

- **Jetpack Compose / ViewBinding**: UI development (currently using ViewBinding).
- **Navigation Component**: For managing app navigation and fragment transactions.
- **Room Persistence Library**: Local database for storing buckets, stocks, and snapshots.
- **WorkManager**: For background tasks.
- **Coroutines & Flow**: For asynchronous programming and reactive data streams.
- **Material Components**: For UI elements and styling.
- **Lifecycle (ViewModel, LiveData/StateFlow)**: For lifecycle-aware data management.

## Features

- Create and manage investment buckets with target percentages.
- Add and update stocks within specific buckets.
- Update stock values via "Buy/Sell" actions.
- Adjust individual stock target allocations.
- Add or remove funds from specific stocks.
- Visual indicators for over-allocated stocks.
