package com.bikeshare.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeshare.app.core.network.StationDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val stations: List<StationDto>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _navigateToRide = MutableSharedFlow<Unit>()
    val navigateToRide: SharedFlow<Unit> = _navigateToRide

    private val _rideCount = MutableStateFlow(0)
    val rideCount: StateFlow<Int> = _rideCount

    init {
        viewModelScope.launch {
            val activeRideCheck = launch {
                if (repository.hasActiveRide()) _navigateToRide.emit(Unit)
            }
            val rideCountFetch = launch {
                _rideCount.value = repository.getRideCount()
            }
            loadStations()
            activeRideCheck.join()
            rideCountFetch.join()
        }
    }

    fun loadStations() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = repository.getStations()) {
                is HomeResult.Success -> _uiState.value = HomeUiState.Success(result.data)
                is HomeResult.Error -> _uiState.value = HomeUiState.Error(result.message)
            }
        }
    }
}
