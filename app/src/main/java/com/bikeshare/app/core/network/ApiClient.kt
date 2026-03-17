package com.bikeshare.app.core.network

import com.bikeshare.app.BuildConfig
import com.bikeshare.app.core.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Attaches "Authorization: Bearer <token>" to every request automatically.
    // If there's no token (user not logged in), the header is simply not added.
    private fun authInterceptor(tokenStorage: TokenStorage): Interceptor = Interceptor { chain ->
        val token = tokenStorage.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // Logs all HTTP requests and responses in debug builds — very useful for
    // seeing exactly what's going over the wire when testing.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY   // full request + response body
        } else {
            HttpLoggingInterceptor.Level.NONE   // silent in production
        }
    }

    fun create(tokenStorage: TokenStorage): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor(tokenStorage))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
