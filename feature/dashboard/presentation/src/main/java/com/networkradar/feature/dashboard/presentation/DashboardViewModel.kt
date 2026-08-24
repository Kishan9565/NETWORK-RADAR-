package com.networkradar.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

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
