package com.pfitzer.tootsie.data.remote.interceptors

/**
 * Provider interface for instance URLs.
 */
interface InstanceUrlProvider {
    suspend fun getInstanceUrl(): String?
}
