package com.pfitzer.tootsie.data.repository

import android.util.Log
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.repository.InstanceRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InstanceRepository"

/**
 * Implementation of InstanceRepository.
 * The limits rarely change, so they are cached per instance for the lifetime of the process.
 */
@Singleton
class InstanceRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService,
    private val instanceUrlProvider: InstanceUrlProvider
) : InstanceRepository {

    private val limitsByInstance = ConcurrentHashMap<String, InstanceLimits>()
    private val translationByInstance = ConcurrentHashMap<String, Boolean>()

    override suspend fun getLimits(): InstanceLimits {
        val instance = instanceUrlProvider.getInstanceUrlSync()
        instance?.let { limitsByInstance[it] }?.let { return it }

        return try {
            val dto = mastodonApi.getInstance()
            val statuses = dto.configuration?.statuses
            val limits = InstanceLimits(
                // Older servers and forks (e.g. Pleroma) only report max_toot_chars
                maxCharacters = statuses?.maxCharacters ?: dto.maxTootChars,
                maxMediaAttachments = statuses?.maxMediaAttachments
                    ?: InstanceLimits.DEFAULT_MAX_MEDIA_ATTACHMENTS,
                charactersReservedPerUrl = statuses?.charactersReservedPerUrl
                    ?: InstanceLimits.DEFAULT_CHARACTERS_RESERVED_PER_URL
            )
            instance?.let { limitsByInstance[it] = limits }
            limits
        } catch (e: Exception) {
            // Not cached, so the next compose screen tries again
            Log.w(TAG, "Failed to load instance limits, using defaults", e)
            InstanceLimits()
        }
    }

    override suspend fun isTranslationAvailable(): Boolean {
        val instance = instanceUrlProvider.getInstanceUrlSync()
        instance?.let { translationByInstance[it] }?.let { return it }

        return try {
            val available = mastodonApi.getInstanceV2().configuration?.translation?.enabled == true
            instance?.let { translationByInstance[it] = available }
            available
        } catch (e: Exception) {
            // Servers without /api/v2/instance (Mastodon < 4.0, some forks) can't translate;
            // not cached, so a network error is retried later
            Log.w(TAG, "Failed to load translation support", e)
            false
        }
    }
}
