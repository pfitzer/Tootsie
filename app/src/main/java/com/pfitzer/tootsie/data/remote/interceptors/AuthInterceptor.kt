package com.pfitzer.tootsie.data.remote.interceptors

import android.util.Log
import com.pfitzer.tootsie.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private const val TAG = "AuthInterceptor"

/**
 * OkHttp interceptor that adds authentication headers to API requests.
 * Retrieves the access token synchronously from memory to avoid blocking.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip authentication for auth endpoints and instance info
        val url = originalRequest.url.toString()
        if (url.contains("/api/v1/apps") ||
            url.contains("/oauth/token") ||
            url.contains("/api/v1/instance")
        ) {
            if (BuildConfig.DEBUG) Log.d(TAG, "intercept() - Skipping auth for: $url")
            return chain.proceed(originalRequest)
        }

        // Requests made on behalf of a specific account carry their own credentials
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        // Get access token synchronously (no blocking)
        val token = tokenProvider.getTokenSync()
        if (BuildConfig.DEBUG) Log.d(TAG, "intercept() - Token present: ${token != null}")

        // Add Authorization header if token is available
        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
