package com.networkradar.core.data.di

import com.google.android.gms.location.LocationServices
import com.networkradar.core.data.indoor.AndroidPdrDataSource
import com.networkradar.core.data.location.AndroidLocationDataSource
import com.networkradar.core.data.measurement.RealScanManager
import com.networkradar.core.data.networking.AndroidCellularDataSource
import com.networkradar.core.data.networking.AndroidConnectivityDataSource
import com.networkradar.core.data.networking.AndroidWifiDataSource
import com.networkradar.core.data.networking.HttpClientFactory
import com.networkradar.core.data.networking.KtorDownloadMeasurementDataSource
import com.networkradar.core.data.networking.KtorInternetLatencyDataSource
import com.networkradar.core.data.networking.KtorUploadMeasurementDataSource
import com.networkradar.core.data.prefs.DataStoreFactory
import com.networkradar.core.domain.indoor.PdrDataSource
import com.networkradar.core.domain.location.LocationDataSource
import com.networkradar.core.domain.measurement.CellularDataSource
import com.networkradar.core.domain.measurement.ConnectivityDataSource
import com.networkradar.core.domain.measurement.DownloadMeasurementDataSource
import com.networkradar.core.domain.measurement.InternetLatencyDataSource
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.UploadMeasurementDataSource
import com.networkradar.core.domain.measurement.WifiDataSource
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule = module {
    single {
        HttpClientFactory().build(OkHttp.create())
    }
    single {
        DataStoreFactory(androidContext()).create()
    }
    single {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }
    singleOf(::AndroidLocationDataSource) bind LocationDataSource::class
    singleOf(::AndroidConnectivityDataSource) bind ConnectivityDataSource::class
    singleOf(::AndroidWifiDataSource) bind WifiDataSource::class
    singleOf(::AndroidCellularDataSource) bind CellularDataSource::class
    singleOf(::KtorInternetLatencyDataSource) bind InternetLatencyDataSource::class
    singleOf(::KtorDownloadMeasurementDataSource) bind DownloadMeasurementDataSource::class
    singleOf(::KtorUploadMeasurementDataSource) bind UploadMeasurementDataSource::class
    singleOf(::AndroidPdrDataSource) bind PdrDataSource::class
    singleOf(::RealScanManager) bind ScanManager::class
}
