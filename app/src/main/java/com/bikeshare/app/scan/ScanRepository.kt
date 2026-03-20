package com.bikeshare.app.scan

import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.UnlockRequest
import kotlinx.coroutines.delay

sealed class ScanResult {
    data class UnlockStarted(val requestId: String) : ScanResult()
    object Success : ScanResult()
    data class Error(val message: String) : ScanResult()
}

class ScanRepository(private val apiService: ApiService) {

    suspend fun unlock(bikeId: String): ScanResult {
        return try {
            val response = apiService.unlock(UnlockRequest(bikeId))
            if (response.isSuccessful) {
                ScanResult.UnlockStarted(response.body()!!.request_id)
            } else {
                ScanResult.Error("Failed to send unlock command.")
            }
        } catch (e: Exception) {
            ScanResult.Error("Network error. Check your connection.")
        }
    }

    // Polls every 2 seconds, up to 10 attempts (~20 seconds total)
    suspend fun pollStatus(requestId: String): ScanResult {
        repeat(10) {
            delay(2000)
            try {
                val response = apiService.getCommandStatus(requestId)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    when (body.status) {
                        "SUCCESS" -> return ScanResult.Success
                        "FAILED"  -> return ScanResult.Error(body.failure_reason ?: "Unlock failed.")
                        "TIMEOUT" -> return ScanResult.Error("Unlock timed out. Try again.")
                    }
                }
            } catch (e: Exception) {
                // network blip — keep polling
            }
        }
        return ScanResult.Error("No response from bike. Please try again.")
    }
}
