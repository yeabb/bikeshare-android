package com.bikeshare.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeshare.app.core.network.CompletedRideResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val rides: List<CompletedRideResponse>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        loadRides()
    }

    fun loadRides() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            when (val result = repository.getRides()) {
                is HistoryResult.Success -> _uiState.value = HistoryUiState.Success(result.rides)
                is HistoryResult.Error -> _uiState.value = HistoryUiState.Error(result.message)
            }
        }
    }
}
