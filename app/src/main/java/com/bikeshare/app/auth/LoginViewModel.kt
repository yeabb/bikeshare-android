package com.bikeshare.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Represents every possible state the Login screen can be in.
// The screen observes this and redraws itself whenever it changes.
sealed class LoginUiState {
    object Idle : LoginUiState()           // nothing happening, form is empty
    object Loading : LoginUiState()        // waiting for API response
    data class Success(val phone: String, val debugOtp: String?) : LoginUiState()  // OTP sent
    data class Error(val message: String) : LoginUiState()  // something went wrong
}

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            when (val result = repository.requestOtp(phone)) {
                is AuthResult.Success -> {
                    // debugOtp is only returned by the backend in DEBUG mode.
                    // In production this will be null — user gets a real SMS instead.
                    _uiState.value = LoginUiState.Success(
                        phone = phone,
                        debugOtp = result.data.ifBlank { null }
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = LoginUiState.Error(result.message)
                }
            }
        }
    }
}
