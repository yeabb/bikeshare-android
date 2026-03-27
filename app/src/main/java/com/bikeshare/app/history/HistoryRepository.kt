package com.bikeshare.app.history

import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.CompletedRideResponse

sealed class HistoryResult {
    data class Success(val rides: List<CompletedRideResponse>) : HistoryResult()
    data class Error(val message: String) : HistoryResult()
}

class HistoryRepository(private val apiService: ApiService) {

    suspend fun getRides(): HistoryResult {
        return try {
            val response = apiService.getRides()
            if (response.isSuccessful) {
                HistoryResult.Success(response.body()?.rides ?: emptyList())
            } else {
                HistoryResult.Error("Failed to load ride history.")
            }
        } catch (e: Exception) {
            HistoryResult.Error("Network error. Check your connection.")
        }
    }
}
