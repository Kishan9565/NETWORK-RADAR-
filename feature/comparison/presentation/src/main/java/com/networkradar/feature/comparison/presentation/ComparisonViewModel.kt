package com.networkradar.feature.comparison.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.comparison.domain.CompareScansUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComparisonViewModel(
    private val scanSessionDataSource: ScanSessionLocalDataSource,
    private val compareScansUseCase: CompareScansUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ComparisonState())
    val state = combine(
        scanSessionDataSource.getAllSessions(),
        _state
    ) { scans, state ->
        state.copy(scans = scans)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComparisonState()
    )

    private val _events = Channel<ComparisonEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ComparisonAction) {
        when (action) {
            is ComparisonAction.SelectScanA -> {
                _state.update { it.copy(selectedIdA = action.id, comparison = null) }
            }
            is ComparisonAction.SelectScanB -> {
                _state.update { it.copy(selectedIdB = action.id, comparison = null) }
            }
            ComparisonAction.Compare -> {
                performComparison()
            }
            ComparisonAction.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun performComparison() {
        val idA = _state.value.selectedIdA
        val idB = _state.value.selectedIdB

        if (idA == null || idB == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = compareScansUseCase(idA, idB)
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(comparison = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    val errorMsg = "Comparison failed: ${result.error}"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _events.send(ComparisonEvent.Error(errorMsg))
                }
            }
        }
    }
}
