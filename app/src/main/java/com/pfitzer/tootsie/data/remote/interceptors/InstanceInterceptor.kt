package com.pfitzer.tootsie.data.remote.interceptors

import android.util.Log
import com.pfitzer.tootsie.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private const val TAG = "InstanceInterceptor"

/**
 * OkHttp interceptor that dynamically sets the base URL based on the current instance.
 * This allows the app to connect to different Mastodon instances.
 */
class InstanceInterceptor @Inject constructor(
    private val instanceUrlProvider: InstanceUrlProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (BuildConfig.DEBUG) Log.d(TAG, "intercept() - Original URL: ${originalRequest.url}")

        // Get current instance URL synchronously
        val instance = instanceUrlProvider.getInstanceUrlSync() ?: "mastodon.social"
        if (BuildConfig.DEBUG) Log.d(TAG, "intercept() - Instance from provider: $instance")

        // Build new URL with the correct instance
        val newUrl = HttpUrl.Builder()
            .scheme("https")
            .host(instance)
            .build()

        // Replace the host in the original request
        val finalUrl = originalRequest.url.newBuilder()
            .scheme(newUrl.scheme)
            .host(newUrl.host)
            .build()

        val request = originalRequest.newBuilder()
            .url(finalUrl)
            .build()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "intercept() - Final URL: ${request.url}")
            Log.d(TAG, "intercept() - Making network request...")
        }

        try {
            val response = chain.proceed(request)
            if (BuildConfig.DEBUG) Log.d(TAG, "intercept() - Response received: ${response.code}")
            return response
        } catch (e: Exception) {
            Log.e(TAG, "intercept() - Network request failed", e)
            throw e
        }
    }
}
