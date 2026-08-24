package com.networkradar.feature.comparison.presentation.di

import com.networkradar.feature.comparison.domain.CompareScansUseCase
import com.networkradar.feature.comparison.presentation.ComparisonViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val comparisonPresentationModule = module {
    singleOf(::CompareScansUseCase)
    viewModelOf(::ComparisonViewModel)
}
