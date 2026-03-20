package com.bikeshare.app.home

import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.StationDto

sealed class HomeResult<out T> {
    data class Success<T>(val data: T) : HomeResult<T>()
    data class Error(val message: String) : HomeResult<Nothing>()
}

class HomeRepository(
    private val apiService: ApiService,
) {
    suspend fun getStations(): HomeResult<List<StationDto>> {
        return try {
            val response = apiService.getStations()
            if (response.isSuccessful) {
                HomeResult.Success(response.body() ?: emptyList())
            } else {
                HomeResult.Error("Failed to load stations.")
            }
        } catch (e: Exception) {
            HomeResult.Error("Network error. Check your connection.")
        }
    }

    suspend fun hasActiveRide(): Boolean {
        return try {
            val response = apiService.getActiveRide()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
