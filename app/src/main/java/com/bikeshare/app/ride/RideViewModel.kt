package com.bikeshare.app.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class RideUiState {
    object Loading : RideUiState()
    data class Active(
        val rideId: String,
        val startStation: String,
        val elapsedSeconds: Long,
    ) : RideUiState()
    object Completed : RideUiState()
    data class Error(val message: String) : RideUiState()
}

class RideViewModel(private val repository: RideRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RideUiState>(RideUiState.Loading)
    val uiState: StateFlow<RideUiState> = _uiState

    init {
        loadRide()
    }

    private fun loadRide() {
        viewModelScope.launch {
            when (val result = repository.getActiveRide()) {
                is RideResult.Active -> {
                    val startTimeMs = parseStartedAt(result.ride.started_at)
                    startTimer(result.ride.ride_id, result.ride.start_station_id, startTimeMs)
                    startPolling()
                }
                is RideResult.NoActiveRide -> _uiState.value = RideUiState.Completed
                is RideResult.Error -> _uiState.value = RideUiState.Error(result.message)
            }
        }
    }

    // Ticks every second and updates elapsedSeconds in the Active state
    private fun startTimer(rideId: String, startStation: String, startTimeMs: Long) {
        viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000
                _uiState.value = RideUiState.Active(
                    rideId = rideId,
                    startStation = startStation,
                    elapsedSeconds = elapsed,
                )
                delay(1000)
            }
        }
    }

    // Polls every 10 seconds — a 404 means the ride ended (bike was docked)
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                when (repository.getActiveRide()) {
                    is RideResult.NoActiveRide -> {
                        _uiState.value = RideUiState.Completed
                        break
                    }
                    else -> {} // still active or network blip — keep polling
                }
            }
        }
    }

    private fun parseStartedAt(startedAt: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(startedAt)?.time ?: continue
            } catch (e: Exception) {
                continue
            }
        }
        return System.currentTimeMillis()
    }
}
