package com.bikeshare.app.ride

import com.bikeshare.app.core.network.ActiveRideResponse
import com.bikeshare.app.core.network.ApiService
import com.bikeshare.app.core.network.CompletedRideResponse

sealed class RideResult {
    data class Active(val ride: ActiveRideResponse) : RideResult()
    object NoActiveRide : RideResult()
    data class Error(val message: String) : RideResult()
}

sealed class CompletedRideResult {
    data class Success(val ride: CompletedRideResponse) : CompletedRideResult()
    data class Error(val message: String) : CompletedRideResult()
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

    suspend fun getCompletedRide(rideId: String): CompletedRideResult {
        return try {
            val response = apiService.getRide(rideId)
            if (response.isSuccessful) {
                CompletedRideResult.Success(response.body()!!)
            } else {
                CompletedRideResult.Error("Failed to load ride summary.")
            }
        } catch (e: Exception) {
            CompletedRideResult.Error("Network error.")
        }
    }
}
