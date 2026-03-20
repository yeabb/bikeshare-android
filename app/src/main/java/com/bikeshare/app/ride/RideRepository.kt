package com.bikeshare.app.ride

import com.bikeshare.app.core.network.ActiveRideResponse
import com.bikeshare.app.core.network.ApiService

sealed class RideResult {
    data class Active(val ride: ActiveRideResponse) : RideResult()
    object NoActiveRide : RideResult()
    data class Error(val message: String) : RideResult()
}

class RideRepository(private val apiService: ApiService) {

    suspend fun getActiveRide(): RideResult {
        return try {
            val response = apiService.getActiveRide()
            when {
                response.isSuccessful -> RideResult.Active(response.body()!!)
                response.code() == 404 -> RideResult.NoActiveRide
                else -> RideResult.Error("Failed to load ride.")
            }
        } catch (e: Exception) {
            RideResult.Error("Network error.")
        }
    }
}
