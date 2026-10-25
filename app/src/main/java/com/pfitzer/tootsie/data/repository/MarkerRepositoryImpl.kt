package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.repository.MarkerRepository
import com.pfitzer.tootsie.domain.repository.MarkerTimeline
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MarkerRepository.
 */
@Singleton
class MarkerRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : MarkerRepository {

    override suspend fun getMarker(timeline: MarkerTimeline): Result<String?> {
        return try {
            val markers = mastodonApi.getMarkers(listOf(timeline.apiName))
            Result.Success(markers[timeline.apiName]?.lastReadId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load marker", e)
        }
    }

    override suspend fun saveMarker(timeline: MarkerTimeline, lastReadId: String): Result<Unit> {
        return try {
            mastodonApi.saveMarkers(mapOf("${timeline.apiName}[last_read_id]" to lastReadId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save marker", e)
        }
    }
}
