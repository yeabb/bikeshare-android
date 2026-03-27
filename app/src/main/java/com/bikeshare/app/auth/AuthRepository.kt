package com.bikeshare.app.auth

import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.RequestOtpRequest
import com.bikeshare.app.core.network.UpdateProfileRequest
import com.bikeshare.app.core.network.VerifyOtpRequest
import com.bikeshare.app.core.storage.TokenStorage

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
                val otp = response.body()?.otp
                AuthResult.Success(otp ?: "")
            } else {
                AuthResult.Error("Failed to send OTP. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error. Check your connection.")
        }
    }

    // Returns true if the user still needs to set their name (first sign-up).
    suspend fun verifyOtp(phone: String, otp: String): AuthResult<Boolean> {
        return try {
            val response = apiService.verifyOtp(VerifyOtpRequest(phone, otp))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenStorage.saveTokens(body.access, body.refresh)
                val name = body.user.name
                val nameRequired = name.isNullOrBlank()
                if (!nameRequired) tokenStorage.saveName(name!!)
                AuthResult.Success(nameRequired)
            } else {
                AuthResult.Error("Invalid OTP. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error. Check your connection.")
        }
    }

    suspend fun setName(name: String): AuthResult<Unit> {
        return try {
            val response = apiService.updateProfile(UpdateProfileRequest(name))
            if (response.isSuccessful) {
                tokenStorage.saveName(name)
                AuthResult.Success(Unit)
            } else {
                AuthResult.Error("Failed to save name. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error. Check your connection.")
        }
    }
}
