package com.networkradar.di

import android.content.Context
import com.networkradar.core.data.di.coreDataModule
import com.networkradar.core.database.di.coreDatabaseModule
import com.networkradar.feature.comparison.presentation.di.comparisonPresentationModule
import com.networkradar.feature.dashboard.presentation.di.dashboardPresentationModule
import com.networkradar.feature.heatmap.presentation.di.heatmapPresentationModule
import com.networkradar.feature.history.presentation.di.historyPresentationModule
import com.networkradar.feature.map.presentation.di.mapPresentationModule
import com.networkradar.feature.radar.presentation.di.radarPresentationModule
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

class KoinModuleTest : KoinTest {

    @Test
    fun `verify koin modules`() {
        koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(
                coreDataModule,
                coreDatabaseModule,
                dashboardPresentationModule,
                radarPresentationModule,
                mapPresentationModule,
                heatmapPresentationModule,
                historyPresentationModule,
                comparisonPresentationModule
            )
        }.checkModules()
    }
}
