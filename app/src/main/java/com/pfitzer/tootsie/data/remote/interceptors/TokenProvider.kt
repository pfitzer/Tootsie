package com.pfitzer.tootsie.data.remote.interceptors

/**
 * Provider interface for access tokens.
 */
interface TokenProvider {
    suspend fun getToken(): String?
}
