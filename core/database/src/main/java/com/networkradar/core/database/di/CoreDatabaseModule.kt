package com.networkradar.core.database.di

import com.networkradar.core.database.NetworkRadarDatabase
import com.networkradar.core.database.RoomDatabaseFactory
import com.networkradar.core.database.RoomIndoorMapDataSource
import com.networkradar.core.database.RoomNetworkMeasurementPointDataSource
import com.networkradar.core.database.RoomScanSessionDataSource
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDatabaseModule = module {
    single { RoomDatabaseFactory(androidContext()) }
    single {
        get<RoomDatabaseFactory>().create(
            NetworkRadarDatabase::class.java,
            "network_radar.db"
        )
    }
    
    single { get<NetworkRadarDatabase>().indoorMapDao() }
    single { get<NetworkRadarDatabase>().scanSessionDao() }
    single { get<NetworkRadarDatabase>().measurementPointDao() }

    singleOf(::RoomIndoorMapDataSource) bind IndoorMapLocalDataSource::class
    singleOf(::RoomScanSessionDataSource) bind ScanSessionLocalDataSource::class
    singleOf(::RoomNetworkMeasurementPointDataSource) bind NetworkMeasurementPointLocalDataSource::class
}
