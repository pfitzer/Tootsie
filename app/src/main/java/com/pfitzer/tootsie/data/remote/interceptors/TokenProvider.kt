package com.pfitzer.tootsie.data.remote.interceptors

import java.util.concurrent.atomic.AtomicReference

/**
 * Provider interface for access tokens.
 * Uses synchronous access to avoid blocking OkHttp threads.
 */
interface TokenProvider {
    /**
     * Gets the current access token synchronously.
     * Returns null if no token is set.
     */
    fun getTokenSync(): String?

    /**
     * Sets the access token (can be called from any thread).
     */
    fun setToken(token: String?)

    /**
     * Clears the access token.
     */
    fun clearToken()
}
