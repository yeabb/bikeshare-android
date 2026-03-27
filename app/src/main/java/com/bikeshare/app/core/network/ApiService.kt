package com.bikeshare.app.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

// ---------- Request bodies ----------

data class RequestOtpRequest(val phone: String)

data class VerifyOtpRequest(val phone: String, val otp: String)

data class TokenRefreshRequest(val refresh: String)

data class UnlockRequest(val bike_id: String)

data class UpdateProfileRequest(val name: String)

// ---------- Response bodies ----------

data class RequestOtpResponse(val message: String, val otp: String?)

data class UserDto(val id: String, val phone: String, val name: String?)

data class VerifyOtpResponse(val access: String, val refresh: String, val user: UserDto)

data class TokenRefreshResponse(val access: String)

data class CommandStatusResponse(
    val request_id: String,
    val status: String,           // PENDING, SUCCESS, FAILED, TIMEOUT
    val failure_reason: String?,
    val ride_id: String?,
)

data class ActiveRideResponse(
    val ride_id: String,
    val status: String,
    val started_at: String,
    val start_station_id: String,
    val start_station_name: String,
)

data class CompletedRideResponse(
    val ride_id: String,
    val start_station_name: String,
    val end_station_name: String?,
    val duration_sec: Int?,
    val started_at: String,
    val status: String,
)

data class RideListResponse(val rides: List<CompletedRideResponse>)

data class UpdateProfileResponse(val name: String)

data class StationDto(
    val station_id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val status: String,
    val available_bikes: Int,
    val open_docks: Int,
    val bike_ids: List<String> = emptyList(),
)

// ---------- Endpoints ----------

interface ApiService {

    // Auth
    @POST("auth/request-otp/")
    suspend fun requestOtp(@Body body: RequestOtpRequest): Response<RequestOtpResponse>

    @POST("auth/verify-otp/")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body body: TokenRefreshRequest): Response<TokenRefreshResponse>

    // Commands
    @POST("commands/unlock/")
    suspend fun unlock(@Body body: UnlockRequest): Response<CommandStatusResponse>

    @GET("commands/{request_id}/")
    suspend fun getCommandStatus(@Path("request_id") requestId: String): Response<CommandStatusResponse>

    // Stations
    @GET("stations/")
    suspend fun getStations(): Response<List<StationDto>>

    // Auth — profile
    @PATCH("auth/me/")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UpdateProfileResponse>

    // Rides
    @GET("me/active-ride/")
    suspend fun getActiveRide(): Response<ActiveRideResponse>

    @GET("me/rides/")
    suspend fun getRides(): Response<RideListResponse>

    @GET("me/rides/{ride_id}/")
    suspend fun getRide(@Path("ride_id") rideId: String): Response<CompletedRideResponse>
}
