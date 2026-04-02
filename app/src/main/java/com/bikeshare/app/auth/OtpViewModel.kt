package com.bikeshare.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    data class Success(val nameRequired: Boolean) : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}

// Separate state for the resend button — independent of OTP verification state
sealed class ResendState {
    object Idle : ResendState()
    object Loading : ResendState()
    object Sent : ResendState()
    data class Error(val message: String) : ResendState()
}

class OtpViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val uiState: StateFlow<OtpUiState> = _uiState

    private val _resendState = MutableStateFlow<ResendState>(ResendState.Idle)
    val resendState: StateFlow<ResendState> = _resendState

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = OtpUiState.Loading

            when (val result = repository.verifyOtp(phone, otp)) {
                is AuthResult.Success -> _uiState.value = OtpUiState.Success(result.data)
                is AuthResult.Error -> _uiState.value = OtpUiState.Error(result.message)
            }
        }
    }

    fun resendOtp(phone: String) {
        if (_resendState.value is ResendState.Loading) return
        viewModelScope.launch {
            _resendState.value = ResendState.Loading
            when (val result = repository.requestOtp(phone)) {
                is AuthResult.Success -> {
                    _resendState.value = ResendState.Sent
                    delay(3_000)
                    _resendState.value = ResendState.Idle
                }
                is AuthResult.Error -> {
                    _resendState.value = ResendState.Error(result.message)
                    delay(3_000)
                    _resendState.value = ResendState.Idle
                }
            }
        }
    }
}
