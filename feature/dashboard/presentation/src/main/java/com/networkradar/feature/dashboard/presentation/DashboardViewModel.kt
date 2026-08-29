package com.networkradar.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val sessionDataSource: ScanSessionLocalDataSource
) : ViewModel() {

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

    val state = sessionDataSource.getAllSessions()
        .map { sessions ->
            DashboardState(
                lastScan = sessions.maxByOrNull { it.startedAt },
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardState(isLoading = true)
        )

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.StartScan -> {
                viewModelScope.launch {
                    _events.send(DashboardEvent.NavigateToRadar)
                }
            }
            DashboardAction.ViewHistory -> {
                viewModelScope.launch {
                    _events.send(DashboardEvent.NavigateToHistory)
                }
            }
            DashboardAction.ManageMaps -> {
                viewModelScope.launch {
                    _events.send(DashboardEvent.NavigateToMaps)
                }
            }
            DashboardAction.CompareScans -> {
                viewModelScope.launch {
                    _events.send(DashboardEvent.NavigateToComparison)
                }
            }
        }
    }
}
