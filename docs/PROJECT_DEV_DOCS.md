# Network Radar - Engineering Documentation

## PROJECT OVERVIEW
- **Project name:** Network Radar
- **Product purpose:** Spatial connectivity intelligence platform.
- **Product vision:** To provide real-world insights into network performance (Internet, RF, Wi-Fi, Cellular) combined with physical location.
- **Current phase:** Phase 9 — Final Polish & Verification

## ARCHITECTURE
The project follows **Clean Architecture** principles combined with **feature-layered modularization**.

### Dependency Direction
- `presentation` -> `domain`
- `data` -> `domain`
- `domain` -> `core:domain`
*Reverse dependencies are strictly forbidden.*

### Core Modules
- `:core:domain`: Pure Kotlin module containing shared entities, business rules, and error handling. No Android dependencies.
- `:core:data`: Shared data infrastructure (Ktor, DataStore, Location, Indoor Mapping).
- `:core:presentation`: Shared UI components and presentation utilities.
- `:core:design-system`: Centralized Material 3 theme, typography, and colors.
- `:core:database`: Room database boundary.

## MODULE MAP
- `:app`: Application module, assembles DI and Navigation.
- `:core:*`: Shared infrastructure modules.
- `:feature:dashboard`: Main entry point and overview.
- `:feature:radar`: Real-time measurement interface and observation engine.
- `:feature:speedtest`: Network throughput testing engine.
- `:feature:map`: Indoor floor-plan management and spatial positioning.
- `:feature:scan`: RF/Wi-Fi discovery.
- `:feature:heatmap`: Signal density visualization.
- `:feature:history`: Past measurement sessions and reporting.
- `:feature:comparison`: Network benchmarking and comparison.

## TECH STACK
- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **DI:** Koin
- **Navigation:** Compose Navigation with Type-Safe Routes (Kotlin Serialization)
- **Async:** Coroutines, Flow
- **Networking:** Ktor
- **Persistence:** Room, DataStore
- **Location:** Google Play Services Fused Location Provider
- **Testing:** JUnit5, AssertK, Turbine, Coroutines Test, Ktor Mock Engine, MockK

## DI
Koin is used for dependency injection. Modules are defined within features and infrastructure, and assembled in the `NetworkRadarApp` class.
- Uses constructor-reference APIs (`viewModelOf`, `singleOf`).
- `coreDataModule` provides singleton `HttpClient`, `DataStore`, `FusedLocationProviderClient`, `IndoorDataSource`, `ScanManager`, and measurement data sources.
- `coreDatabaseModule` provides the singleton `NetworkRadarDatabase` and its DAOs, along with Room-backed data source implementations.

## PRESENTATION (MVI)
Each feature follows the MVI pattern:
- **State:** Immutable data class representing the UI state.
- **Action:** Sealed interface for user interactions.
- **Event:** Sealed interface for one-time side effects.
- **ViewModel:** Manages state and events, handles actions.
- **Root/Screen:** Separation of Koin-injected root and pure, previewable screen composables.

## NAVIGATION
- **Type-safe routes:** Defined using `@Serializable` objects.
- **Feature graphs:** Features expose their own `NavGraphBuilder` extensions.
- **App graph:** Assembled in `MainActivity` via `NavHost`.

## DATA INFRASTRUCTURE
### Result & Error Handling
- `Result<D, E>` sealed interface for handling success and failure.
- `DataError` defines common `Network` and `Local` error types.
- `safeCall` helper in `:core:data` maps Ktor exceptions and HTTP status codes to `DataError.Network`.

### Networking
- Centralized `HttpClientFactory` using Ktor with OkHttp engine.
- Shared extensions for `get`, `post`, `delete` with type-safe response handling.

### Persistence (Phase 6)
- **Database:** Single Room database boundary `NetworkRadarDatabase`.
- **Entities:** `IndoorMapEntity`, `ScanSessionEntity`, `NetworkMeasurementPointEntity`.
- **DAOs:** Dedicated interfaces for Map, Session, and Point operations with reactive Flow support.
- **Mappers:** Located in `:core:database:mappers`, providing bidirectional conversion between Room Entities and Domain Models without leaking DB annotations.
- **Offline-First:** Room acts as the local source of truth for all measurement data.

### Location (Phase 4)
- **Source:** Google Play Services `FusedLocationProviderClient`.
- **Strategy:** Priority high accuracy, 5s interval, 1m displacement.

### Indoor Mapping (Phase 5)
- **Strategy:** Manual user positioning on a logical 2D coordinate system. 
- **Coordinate System:** Origin (0,0) at top-left, meters as logical units.

### Scan Lifecycle (Phase 6)
- **ScanManager:** Orchestrates the lifecycle of a scan. 
- **Flow:** `startScan` creates a persistent session -> `recordMeasurement` persists points associated with that session -> `stopScan` finalizes the session.

### Intelligence Layer (Phase 7)
- **IntelligenceEngine:** Pure Kotlin engine for analyzing scan data.
- **Statistics:** Calculates count, min, max, average, and median for internet and signal metrics.
- **Stability:** Determined using Coefficient of Variation (CV). CV < 0.1 is Stable, < 0.3 is Moderately Stable.
- **Confidence:** A score from 0.0 to 1.0 based on data sufficiency (sample size) and metric stability.
- **Best/Worst Spot:** Uses a weighted scoring model (Download 40%, Latency 40%, Signal 20%) to rank points. Tie-breaking uses the earliest timestamp.
- **Data Sufficiency:** Categorizes scans into NoData, Insufficient, Valid, or High Confidence based on valid point counts and metric coverage.

### Visualization Layer (Phase 8)
- **Heatmap Engine:** Pure Kotlin engine implementing IDW (Inverse Distance Weighting) interpolation.
- **IDW Formula:** value(x) = Σ(value_i * weight_i) / Σ(weight_i), where weight_i = 1 / distance_i^p.
- **Grid Generation:** Deterministic spatial grid generation based on IndoorMap dimensions.
- **Metric Extraction:** Metric-aware extraction and normalization (Higher-is-better vs Lower-is-better).
- **Compose Canvas:** Efficient rendering of HeatmapCells using Compose Canvas, aligned with Phase 5 IndoorMap coordinate system.
- **Real-Data Only:** Visualization is strictly derived from persisted measurement points; no synthetic data generation.

### History & Comparison Layer (Phase 9)
- **History:** Reactive retrieval of persisted `ScanSession` records from Room, ordered newest first.
- **Reporting:** Reuses `IntelligenceEngine` to provide deep analysis of historical scans including Best/Worst spot identification.
- **Export:** Serializes persisted measurement points to CSV and JSON formats for external analysis, preserving all real null values.
- **Comparison:** Deterministic comparison between two scan sessions, calculating absolute and percentage differences for Download, Latency, and Signal metrics with trend analysis.

## MEASUREMENT ENGINE
### Internet Measurement Layer (Phase 3)
- **Latency:** Real-time round-trip time measurement.
- **Download:** Stream-based throughput calculation (Mbps).
- **Upload:** Currently returns `SERVICE_UNAVAILABLE`.

### RF / Network Layer (Phase 3)
- **Wi-Fi / Cellular / Connectivity:** Real-time transport and signal observation.

## TESTING
- **Unit Testing:** JUnit5 + AssertK + Turbine.
- **Coverage:** 
    - `IntelligenceEngineTest` verifies the full intelligence pipeline.
    - `HeatmapEngineTest` verifies IDW interpolation, grid boundaries, and sparse data handling.
    - `MappersTest` verifies bidirectional mapping for all persistent models.
    - `ExportScanUseCaseTest` verifies CSV/JSON serialization.
    - `CompareScansUseCaseTest` verifies benchmarking logic.

## PHASE 1 STATUS
- [x] Project foundation
- [x] Build Logic
- [x] Modular architecture
- [x] Compose / Design System
- [x] Koin
- [x] MVI
- [x] Navigation
- [x] Testing foundation
- [x] Build verification

## PHASE 2 STATUS
- [x] Core Domain (Result, Error, DataError)
- [x] Data Infrastructure foundation
- [x] Koin Data Wiring
- [x] Infrastructure Unit Tests
- [x] Build Verification

## PHASE 3 STATUS
- [x] Real connectivity/RF measurement engines
- [x] Real Internet performance measurement (Latency/Download)
- [x] MVI Integration (Radar)
- [x] Build Verification

## PHASE 4 STATUS
- [x] Real Android location acquisition (GPS)
- [x] Location availability & accuracy handling
- [x] Outdoor MeasurementPoint composition
- [x] Build Verification

## PHASE 5 STATUS
- [x] Indoor map & position domain models
- [x] Floor-plan Compose workspace
- [x] Manual position selection & validation
- [x] Build Verification

## PHASE 6 STATUS
- [x] Room database boundary
- [x] IndoorMap, ScanSession, NetworkMeasurementPoint persistence
- [x] Scan lifecycle management (ScanManager)
- [x] Measurement recording & session count consistency
- [x] Entity ↔ Domain mappers
- [x] Offline-first data flow
- [x] Build Verification

## PHASE 7 STATUS
- [x] Intelligence domain models
- [x] Pure Kotlin IntelligenceEngine
- [x] Deterministic statistics & stability analysis
- [x] Data sufficiency & confidence scoring
- [x] Best/Worst spot ranking
- [x] AnalyzeScanUseCase integration
- [x] Radar MVI intelligence summary
- [x] Build Verification
- [x] Intelligence Unit Tests

## PHASE 8 STATUS
- [x] Heatmap domain models (HeatmapCell, HeatmapMetric)
- [x] Pure Kotlin IDW HeatmapEngine
- [x] Grid generation & spatial interpolation
- [x] GenerateHeatmapUseCase implementation
- [x] Heatmap MVI (State, ViewModel, Actions)
- [x] Compose HeatmapCanvas rendering
- [x] Metric-aware normalization & legends
- [x] Build Verification
- [x] Heatmap Unit Tests

## PHASE 9 STATUS
- [x] Scan History retrieval (Real Data)
- [x] Scan Details / Report integration
- [x] Export (CSV / JSON)
- [x] Network Comparison / Benchmarking
- [x] MVI / Navigation integration
- [x] Final Build Verification
- [x] Phase 9 Unit Tests
- [x] Regression Audit
- [x] Documentation Update

## NEXT PHASE
Project Complete.
