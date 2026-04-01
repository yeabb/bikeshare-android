package com.bikeshare.app.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Scanning : ScanUiState()                    // camera active, waiting for QR
    object Unlocking : ScanUiState()                   // request sent, polling for result
    data class Error(val message: String) : ScanUiState()
}

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState

    // One-shot event — tells the screen to navigate to the Ride screen
    private val _navigateToRide = MutableSharedFlow<Unit>()
    val navigateToRide: SharedFlow<Unit> = _navigateToRide

    // Prevents processing the same QR frame multiple times while a request is in-flight
    private var isProcessing = false

    fun onBikeIdScanned(bikeId: String) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            _uiState.value = ScanUiState.Unlocking

            when (val unlockResult = repository.unlock(bikeId)) {
                is ScanResult.UnlockStarted -> {
                    when (val pollResult = repository.pollStatus(unlockResult.requestId)) {
                        is ScanResult.Success -> _navigateToRide.emit(Unit)
                        is ScanResult.Error   -> {
                            _uiState.value = ScanUiState.Error(pollResult.message)
                            isProcessing = false
                        }
                        else -> {
                            _uiState.value = ScanUiState.Error("Something went wrong. Please try again.")
                            isProcessing = false
                        }
                    }
                }
                is ScanResult.Error -> {
                    _uiState.value = ScanUiState.Error(unlockResult.message)
                    isProcessing = false
                }
                else -> {
                    _uiState.value = ScanUiState.Error("Something went wrong. Please try again.")
                    isProcessing = false
                }
            }
        }
    }

    fun retry() {
        isProcessing = false
        _uiState.value = ScanUiState.Scanning
    }
}
