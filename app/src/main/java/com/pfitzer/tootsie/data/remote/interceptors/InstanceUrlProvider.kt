package com.pfitzer.tootsie.data.remote.interceptors

import java.util.concurrent.atomic.AtomicReference

/**
 * Provider interface for instance URLs.
 * Uses synchronous access to avoid blocking OkHttp threads.
 */
interface InstanceUrlProvider {
    /**
     * Gets the current instance URL synchronously.
     * Returns null if no instance is set.
     */
    fun getInstanceUrlSync(): String?

    /**
     * Sets the instance URL (can be called from any thread).
     */
    fun setInstanceUrl(url: String)
}

/**
 * Thread-safe implementation of InstanceUrlProvider.
 * Stores instance URL in memory for fast, synchronous access.
 */
class InstanceUrlProviderImpl @javax.inject.Inject constructor() : InstanceUrlProvider {
    private val instanceUrl = AtomicReference<String?>(null)

    override fun getInstanceUrlSync(): String? = instanceUrl.get()

    override fun setInstanceUrl(url: String) {
        instanceUrl.set(url)
    }
}
