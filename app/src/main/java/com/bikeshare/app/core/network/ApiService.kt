package com.bikeshare.app.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// ---------- Request bodies ----------

data class RequestOtpRequest(val phone: String)

data class VerifyOtpRequest(val phone: String, val otp: String)

data class TokenRefreshRequest(val refresh: String)

data class UnlockRequest(val bike_id: String)

// ---------- Response bodies ----------

data class RequestOtpResponse(val message: String, val otp: String?)

data class UserDto(val id: String, val phone: String)

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
    val start_station: String,
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

    // Rides
    @GET("me/active-ride/")
    suspend fun getActiveRide(): Response<ActiveRideResponse>
}
