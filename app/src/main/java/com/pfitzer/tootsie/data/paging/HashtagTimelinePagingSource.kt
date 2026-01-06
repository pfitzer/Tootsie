package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "HashtagTimelinePagingSource"

/**
 * PagingSource for Hashtag Timeline data.
 * Fetches statuses containing a specific hashtag with pagination support.
 */
class HashtagTimelinePagingSource(
    private val mastodonApi: MastodonApiService,
    private val hashtag: String
) : PagingSource<String, Status>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        return try {
            val maxId = params.key // For pagination, load older posts
            Log.d(TAG, "load() called - hashtag=$hashtag, maxId=$maxId, loadSize=${params.loadSize}")

            // Fetch hashtag timeline from API
            val statusDtos = mastodonApi.getHashtagTimeline(
                hashtag = hashtag,
                maxId = maxId,
                limit = params.loadSize
            )

            Log.d(TAG, "API returned ${statusDtos.size} statuses for hashtag #$hashtag")

            // Convert DTOs to domain models
            val statuses = statusDtos.map { it.toDomain() }

            // Determine next key for pagination
            // Set null if we got fewer items than requested (reached end) or list is empty
            val nextKey = if (statusDtos.size < params.loadSize || statusDtos.isEmpty()) {
                null
            } else {
                statusDtos.lastOrNull()?.id
            }

            LoadResult.Page(
                data = statuses,
                prevKey = null, // Only support forward pagination
                nextKey = nextKey
            )
        } catch (e: IOException) {
            Log.e(TAG, "IOException while loading hashtag timeline", e)
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException while loading hashtag timeline", e)
            LoadResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while loading hashtag timeline", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? {
        // Return null to start from the beginning on refresh
        return null
    }
}
