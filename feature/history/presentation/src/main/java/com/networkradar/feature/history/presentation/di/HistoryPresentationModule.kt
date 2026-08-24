package com.networkradar.feature.history.presentation.di

import com.networkradar.feature.history.domain.DeleteScanUseCase
import com.networkradar.feature.history.domain.ExportScanUseCase
import com.networkradar.feature.history.domain.GetScanHistoryUseCase
import com.networkradar.feature.history.domain.GetScanReportUseCase
import com.networkradar.feature.history.presentation.HistoryViewModel
import com.networkradar.feature.history.presentation.ScanReportViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val historyPresentationModule = module {
    singleOf(::GetScanHistoryUseCase)
    singleOf(::DeleteScanUseCase)
    singleOf(::GetScanReportUseCase)
    singleOf(::ExportScanUseCase)
    viewModelOf(::HistoryViewModel)
    viewModel { (sessionId: String) -> 
        ScanReportViewModel(
            sessionId = sessionId,
            scanSessionDataSource = get(),
            getScanReportUseCase = get(),
            exportScanUseCase = get()
        )
    }
}
