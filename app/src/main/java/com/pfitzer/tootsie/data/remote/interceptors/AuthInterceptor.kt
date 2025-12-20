package com.pfitzer.tootsie.data.remote.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds authentication headers to API requests.
 * Retrieves the access token from DataStore and adds it as a Bearer token.
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
            return chain.proceed(originalRequest)
        }

        // Get access token (blocking call in interceptor)
        val token = runBlocking {
            tokenProvider.getToken()
        }

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
