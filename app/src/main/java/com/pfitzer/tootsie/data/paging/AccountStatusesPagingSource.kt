package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "AccountStatusesPaging"

/**
 * PagingSource for Account Statuses (user timeline).
 * Fetches statuses from a specific account.
 */
class AccountStatusesPagingSource(
    private val mastodonApi: MastodonApiService,
    private val accountId: String,
    private val excludeReplies: Boolean = false,
    private val excludeReblogs: Boolean = false,
    private val onlyMedia: Boolean = false,
    private val pinned: Boolean = false
) : PagingSource<String, Status>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        return try {
            val maxId = params.key // For pagination, load older posts
            Log.d(TAG, "load() called - accountId=$accountId, maxId=$maxId, loadSize=${params.loadSize}")

            // Fetch from API
            val statusDtos = mastodonApi.getAccountStatuses(
                id = accountId,
                maxId = maxId,
                limit = params.loadSize,
                excludeReplies = excludeReplies,
                excludeReblogs = excludeReblogs,
                onlyMedia = onlyMedia,
                pinned = pinned
            )

            Log.d(TAG, "API returned ${statusDtos.size} statuses")

            // Convert to domain models
            val statuses = statusDtos.map { it.toDomain() }

            // Determine next page key (ID of oldest status)
            val nextKey = if (statuses.isEmpty()) {
                null
            } else {
                statuses.last().id
            }

            Log.d(TAG, "Returning ${statuses.size} statuses, nextKey=$nextKey")
            LoadResult.Page(
                data = statuses,
                prevKey = null, // Only load older posts (forward pagination)
                nextKey = nextKey
            )
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException: ${e.code()} - ${e.message}", e)
            LoadResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? {
        // Return null to always start from the beginning on refresh
        return null
    }
}
