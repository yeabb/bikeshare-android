package com.bikeshare.app.core.storage

import android.content.Context
import androidx.core.content.edit

// SharedPreferences is Android's built-in key-value store.
// In a production app we'd use EncryptedSharedPreferences (requires more setup),
// but plain SharedPreferences is fine to get us running.
class TokenStorage(context: Context) {

    private val prefs = context.getSharedPreferences("bikeshare_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun clearTokens() {
        prefs.edit {
            remove(KEY_ACCESS)
            remove(KEY_REFRESH)
        }
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
