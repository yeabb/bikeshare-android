package com.bikeshare.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    object Success : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}

class OtpViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val uiState: StateFlow<OtpUiState> = _uiState

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = OtpUiState.Loading

            when (val result = repository.verifyOtp(phone, otp)) {
                is AuthResult.Success -> _uiState.value = OtpUiState.Success
                is AuthResult.Error -> _uiState.value = OtpUiState.Error(result.message)
            }
        }
    }
}
