package com.networkradar

import android.app.Application
import com.networkradar.core.data.di.coreDataModule
import com.networkradar.core.database.di.coreDatabaseModule
import com.networkradar.feature.comparison.presentation.di.comparisonPresentationModule
import com.networkradar.feature.dashboard.presentation.di.dashboardPresentationModule
import com.networkradar.feature.heatmap.presentation.di.heatmapPresentationModule
import com.networkradar.feature.history.presentation.di.historyPresentationModule
import com.networkradar.feature.radar.presentation.di.radarPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class NetworkRadarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NetworkRadarApp)
            modules(
                coreDataModule,
                coreDatabaseModule,
                dashboardPresentationModule,
                radarPresentationModule,
                heatmapPresentationModule,
                historyPresentationModule,
                comparisonPresentationModule
            )
        }
    }
}
