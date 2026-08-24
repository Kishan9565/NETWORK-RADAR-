package com.networkradar.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.util.onSuccess
import com.networkradar.feature.history.domain.DeleteScanUseCase
import com.networkradar.feature.history.domain.GetScanHistoryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val deleteScanUseCase: DeleteScanUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state = combine(
        getScanHistoryUseCase(),
        _state
    ) { scans, state ->
        state.copy(
            scans = scans,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryState(isLoading = true)
    )

    private val _events = Channel<HistoryEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.DeleteScan -> {
                viewModelScope.launch {
                    deleteScanUseCase(action.sessionId).onSuccess {
                        _events.send(HistoryEvent.ShowMessage("Scan deleted"))
                    }
                }
            }
            is HistoryAction.OpenScan -> {
                viewModelScope.launch {
                    _events.send(HistoryEvent.NavigateToScan(action.sessionId))
                }
            }
            HistoryAction.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }
}
