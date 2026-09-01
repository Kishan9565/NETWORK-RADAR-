package com.networkradar.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.networkradar.core.database.dao.NetworkMeasurementPointDao
import com.networkradar.core.database.dao.ScanSessionDao
import com.networkradar.core.database.dao.SpatialAnnotationDao
import com.networkradar.core.database.entity.NetworkMeasurementPointEntity
import com.networkradar.core.database.entity.ScanSessionEntity
import com.networkradar.core.database.entity.SpatialAnnotationEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        NetworkMeasurementPointEntity::class,
        SpatialAnnotationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NetworkRadarDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun measurementPointDao(): NetworkMeasurementPointDao
    abstract fun spatialAnnotationDao(): SpatialAnnotationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Drop the old indoor_maps table
                db.execSQL("DROP TABLE IF EXISTS indoor_maps")

                // 2. Update scan_sessions table
                // Rename old table
                db.execSQL("ALTER TABLE scan_sessions RENAME TO scan_sessions_old")
                // Create new table
                db.execSQL("""
                    CREATE TABLE scan_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        isSpatial INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER,
                        measurementCount INTEGER NOT NULL
                    )
                """.trimIndent())
                // Migrate data
                db.execSQL("""
                    INSERT INTO scan_sessions (id, isSpatial, name, startedAt, endedAt, measurementCount)
                    SELECT id, (CASE WHEN mapId IS NOT NULL THEN 1 ELSE 0 END), name, startedAt, endedAt, measurementCount
                    FROM scan_sessions_old
                """.trimIndent())
                // Drop old table
                db.execSQL("DROP TABLE scan_sessions_old")

                // 3. Update measurement_points table
                // Rename old table
                db.execSQL("ALTER TABLE measurement_points RENAME TO measurement_points_old")
                // Create new table
                db.execSQL("""
                    CREATE TABLE measurement_points (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        sessionId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationAccuracy REAL,
                        locationTimestamp INTEGER,
                        indoorX REAL,
                        indoorY REAL,
                        indoorTimestamp INTEGER,
                        wifi_rssi INTEGER,
                        wifi_ssid TEXT,
                        wifi_frequency INTEGER,
                        wifi_linkSpeed INTEGER,
                        wifi_timestamp INTEGER,
                        cell_networkType TEXT,
                        cell_rsrp INTEGER,
                        cell_rsrq INTEGER,
                        cell_sinr INTEGER,
                        cell_rssi INTEGER,
                        cell_timestamp INTEGER,
                        net_latencyMs REAL,
                        net_downloadMbps REAL,
                        net_uploadMbps REAL,
                        net_timestamp INTEGER,
                        FOREIGN KEY(sessionId) REFERENCES scan_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Migrate data
                db.execSQL("""
                    INSERT INTO measurement_points (
                        id, sessionId, timestamp, latitude, longitude, locationAccuracy, locationTimestamp,
                        indoorX, indoorY, indoorTimestamp,
                        wifi_rssi, wifi_ssid, wifi_frequency, wifi_linkSpeed, wifi_timestamp,
                        cell_networkType, cell_rsrp, cell_rsrq, cell_sinr, cell_rssi, cell_timestamp,
                        net_latencyMs, net_downloadMbps, net_uploadMbps, net_timestamp
                    )
                    SELECT 
                        id, sessionId, timestamp, latitude, longitude, locationAccuracy, locationTimestamp,
                        indoorX, indoorY, timestamp,
                        wifi_rssi, wifi_ssid, wifi_frequency, wifi_linkSpeed, wifi_timestamp,
                        cell_networkType, cell_rsrp, cell_rsrq, cell_sinr, cell_rssi, cell_timestamp,
                        net_latencyMs, net_downloadMbps, net_uploadMbps, net_timestamp
                    FROM measurement_points_old
                """.trimIndent())
                
                // Drop old table
                db.execSQL("DROP TABLE measurement_points_old")
                
                // Create Index AFTER the table is named correctly
                db.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_points_sessionId ON measurement_points(sessionId)")

                // 4. Create spatial_annotations table
                db.execSQL("""
                    CREATE TABLE spatial_annotations (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        sessionId TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        label TEXT,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES scan_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_spatial_annotations_sessionId ON spatial_annotations(sessionId)")
            }
        }
    }
}
