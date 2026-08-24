package com.networkradar.feature.heatmap.presentation.di

import com.networkradar.feature.heatmap.domain.GenerateHeatmapUseCase
import com.networkradar.feature.heatmap.presentation.HeatmapViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val heatmapPresentationModule = module {
    singleOf(::GenerateHeatmapUseCase)
    viewModelOf(::HeatmapViewModel)
}
