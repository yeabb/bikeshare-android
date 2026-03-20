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

    // Fires once if an active ride is detected on load — tells the screen to navigate to Ride
    private val _navigateToRide = MutableSharedFlow<Unit>()
    val navigateToRide: SharedFlow<Unit> = _navigateToRide

    init {
        viewModelScope.launch {
            // Check for active ride and load stations in parallel
            val activeRideCheck = launch {
                if (repository.hasActiveRide()) {
                    _navigateToRide.emit(Unit)
                }
            }
            loadStations()
            activeRideCheck.join()
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
