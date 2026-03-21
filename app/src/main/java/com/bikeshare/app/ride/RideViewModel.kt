package com.bikeshare.app.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
    data class Summary(
        val startStation: String,
        val endStation: String,
        val durationSec: Int,
    ) : RideUiState()
    object Completed : RideUiState()
    data class Error(val message: String) : RideUiState()
}

class RideViewModel(private val repository: RideRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RideUiState>(RideUiState.Loading)
    val uiState: StateFlow<RideUiState> = _uiState

    // Held in memory so we can fetch the summary when the ride ends
    private var currentRideId: String? = null
    private var timerJob: Job? = null

    init {
        loadRide()
    }

    private fun loadRide() {
        viewModelScope.launch {
            when (val result = repository.getActiveRide()) {
                is RideResult.Active -> {
                    currentRideId = result.ride.ride_id
                    val startTimeMs = parseStartedAt(result.ride.started_at)
                    startTimer(result.ride.ride_id, result.ride.start_station_name, startTimeMs)
                    startPolling()
                }
                is RideResult.NoActiveRide -> _uiState.value = RideUiState.Completed
                is RideResult.Error -> _uiState.value = RideUiState.Error(result.message)
            }
        }
    }

    fun onSummaryDismissed() {
        _uiState.value = RideUiState.Completed
    }

    // Ticks every second and updates elapsedSeconds in the Active state
    private fun startTimer(rideId: String, startStation: String, startTimeMs: Long) {
        // Cap to now so that any parsing/clock-skew issue never produces a future base.
        // If startTimeMs is correct (past), the timer shows actual elapsed time.
        // If it's somehow in the future, the timer counts from 0 instead.
        val safeStartMs = minOf(startTimeMs, System.currentTimeMillis())
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - safeStartMs) / 1000
                _uiState.value = RideUiState.Active(
                    rideId = rideId,
                    startStation = startStation,
                    elapsedSeconds = elapsed, // always ≥ 0, always increasing
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
                        showSummary()
                        break
                    }
                    else -> {} // still active or network blip — keep polling
                }
            }
        }
    }

    private suspend fun showSummary() {
        timerJob?.cancel()
        val rideId = currentRideId ?: run {
            _uiState.value = RideUiState.Completed
            return
        }
        when (val result = repository.getCompletedRide(rideId)) {
            is CompletedRideResult.Success -> {
                val ride = result.ride
                _uiState.value = RideUiState.Summary(
                    startStation = ride.start_station_name,
                    endStation = ride.end_station_name ?: "Unknown",
                    durationSec = ride.duration_sec ?: 0,
                )
            }
            is CompletedRideResult.Error -> _uiState.value = RideUiState.Completed
        }
    }

    private fun parseStartedAt(startedAt: String): Long {
        // Take only "yyyy-MM-ddTHH:mm:ss" (first 19 chars) and treat as UTC.
        // Sub-second precision doesn't matter for the ride timer, and this avoids
        // dealing with microsecond digits or timezone suffix variations.
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(startedAt.take(19))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
