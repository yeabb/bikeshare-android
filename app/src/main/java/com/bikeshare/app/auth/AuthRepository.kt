package com.bikeshare.app.auth

import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.RequestOtpRequest
import com.bikeshare.app.core.network.VerifyOtpRequest
import com.bikeshare.app.core.storage.TokenStorage

// Sealed class represents all possible outcomes of an operation.
// Instead of throwing exceptions, we return a Result object that
// is either Success (with data) or Error (with a message).
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage,
) {
    suspend fun requestOtp(phone: String): AuthResult<String> {
        return try {
            val response = apiService.requestOtp(RequestOtpRequest(phone))
            if (response.isSuccessful) {
                // In debug mode the backend returns the OTP in the response.
                // We pass it back so the ViewModel can pre-fill it during development.
                val otp = response.body()?.otp
                AuthResult.Success(otp ?: "")
            } else {
                AuthResult.Error("Failed to send OTP. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error. Check your connection.")
        }
    }

    suspend fun verifyOtp(phone: String, otp: String): AuthResult<Unit> {
        return try {
            val response = apiService.verifyOtp(VerifyOtpRequest(phone, otp))
            if (response.isSuccessful) {
                val body = response.body()!!
                // Save both tokens to device storage so the user stays logged in
                tokenStorage.saveTokens(body.access, body.refresh)
                AuthResult.Success(Unit)
            } else {
                AuthResult.Error("Invalid OTP. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error. Check your connection.")
        }
    }
}
