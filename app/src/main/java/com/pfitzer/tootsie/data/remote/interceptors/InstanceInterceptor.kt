package com.pfitzer.tootsie.data.remote.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that dynamically sets the base URL based on the current instance.
 * This allows the app to connect to different Mastodon instances.
 */
class InstanceInterceptor @Inject constructor(
    private val instanceProvider: suspend () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get current instance URL
        val instance = runBlocking {
            instanceProvider()
        } ?: "mastodon.social" // Default instance

        // Build new URL with the correct instance
        val newUrl = HttpUrl.Builder()
            .scheme("https")
            .host(instance)
            .build()

        // Replace the host in the original request
        val request = originalRequest.newBuilder()
            .url(
                originalRequest.url.newBuilder()
                    .scheme(newUrl.scheme)
                    .host(newUrl.host)
                    .build()
            )
            .build()

        return chain.proceed(request)
    }
}
