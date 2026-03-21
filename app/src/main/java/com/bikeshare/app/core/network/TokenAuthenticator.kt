package com.bikeshare.app.core.network

import com.bikeshare.app.core.storage.TokenStorage
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

/**
 * OkHttp Authenticator — called automatically whenever a request returns 401.
 *
 * Attempts to refresh the access token using the stored refresh token, then
 * retries the original request with the new token. If the refresh fails (e.g.
 * the refresh token has expired), it clears all stored tokens and signals the
 * app to navigate back to login via [AuthEventBus].
 *
 * Uses a bare OkHttp client (no auth, no authenticator) to call the refresh
 * endpoint so it cannot trigger itself recursively.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String,
) : Authenticator {

    // Bare client — no auth interceptor, no authenticator.
    // Must NOT share the main client to avoid recursive refresh loops.
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        // If this is already a retry, give up — prevents infinite refresh loops
        // when the refresh token itself is invalid or the server keeps rejecting us.
        if (response.request.header("X-Retry-After-Refresh") != null) return null

        val refreshToken = tokenStorage.getRefreshToken() ?: run {
            signOut()
            return null
        }

        val newAccessToken = callRefreshEndpoint(refreshToken) ?: run {
            signOut()
            return null
        }

        tokenStorage.saveTokens(newAccessToken, refreshToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .header("X-Retry-After-Refresh", "true")
            .build()
    }

    private fun callRefreshEndpoint(refreshToken: String): String? {
        return try {
            val body = """{"refresh":"$refreshToken"}"""
                .toRequestBody("application/json".toMediaType())
            val request = okhttp3.Request.Builder()
                .url("${baseUrl}auth/token/refresh/")
                .post(body)
                .build()

            val response = refreshClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val json = JSONObject(response.body?.string() ?: return null)
            json.optString("access").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun signOut() {
        tokenStorage.clearTokens()
        AuthEventBus.emitLogout()
    }
}
