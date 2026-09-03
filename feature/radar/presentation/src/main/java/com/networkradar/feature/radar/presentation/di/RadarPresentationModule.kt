package com.networkradar.feature.radar.presentation.di

import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import com.networkradar.feature.radar.presentation.RadarViewModel
import com.networkradar.feature.speedtest.domain.RunDownloadTestUseCase
import com.networkradar.feature.speedtest.domain.RunLatencyTestUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val radarPresentationModule = module {
    singleOf(::ObserveRadarMeasurementsUseCase)
    singleOf(::AnalyzeScanUseCase)
    singleOf(::RunDownloadTestUseCase)
    singleOf(::RunLatencyTestUseCase)
    viewModelOf(::RadarViewModel)
}
