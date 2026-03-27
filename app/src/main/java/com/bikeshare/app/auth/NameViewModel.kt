package com.bikeshare.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NameUiState {
    object Idle : NameUiState()
    object Loading : NameUiState()
    object Success : NameUiState()
    data class Error(val message: String) : NameUiState()
}

class NameViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NameUiState>(NameUiState.Idle)
    val uiState: StateFlow<NameUiState> = _uiState

    fun setName(name: String) {
        viewModelScope.launch {
            _uiState.value = NameUiState.Loading
            when (val result = repository.setName(name)) {
                is AuthResult.Success -> _uiState.value = NameUiState.Success
                is AuthResult.Error -> _uiState.value = NameUiState.Error(result.message)
            }
        }
    }
}
