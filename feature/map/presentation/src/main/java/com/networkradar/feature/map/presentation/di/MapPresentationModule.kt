package com.networkradar.feature.map.presentation.di

import com.networkradar.feature.map.domain.SetIndoorMapUseCase
import com.networkradar.feature.map.domain.SetIndoorPositionUseCase
import com.networkradar.feature.map.presentation.MapViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mapPresentationModule = module {
    singleOf(::SetIndoorMapUseCase)
    singleOf(::SetIndoorPositionUseCase)
    viewModelOf(::MapViewModel)
}
