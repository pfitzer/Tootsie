package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.InstanceLimits

/**
 * Repository for information about the instance of the active account.
 */
interface InstanceRepository {

    /**
     * Posting limits of the active account's instance. Falls back to Mastodon's defaults
     * if the instance can't be reached.
     */
    suspend fun getLimits(): InstanceLimits

    /**
     * Whether the active account's instance can translate statuses (false if unknown).
     */
    suspend fun isTranslationAvailable(): Boolean
}
