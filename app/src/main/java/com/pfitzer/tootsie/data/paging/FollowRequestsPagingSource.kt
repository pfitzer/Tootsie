package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "FollowRequestsPaging"

/**
 * PagingSource for Follow Requests data.
 * Fetches pending follow requests from the Mastodon API with pagination support.
 */
class FollowRequestsPagingSource(
    private val mastodonApi: MastodonApiService
) : PagingSource<String, Account>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Account> {
        return try {
            val maxId = params.key
            Log.d(TAG, "load() called - maxId=$maxId, loadSize=${params.loadSize}")

            // Fetch follow requests from API
            val accountDtos = mastodonApi.getFollowRequests(
                maxId = maxId,
                limit = params.loadSize
            )

            Log.d(TAG, "API returned ${accountDtos.size} follow requests")

            // Convert DTOs to domain models
            val accounts = accountDtos.map { it.toDomain() }

            // Determine next key for pagination
            val nextKey = if (accountDtos.size < params.loadSize || accountDtos.isEmpty()) {
                null
            } else {
                accountDtos.lastOrNull()?.id
            }

            LoadResult.Page(
                data = accounts,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: IOException) {
            Log.e(TAG, "IOException while loading follow requests", e)
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException while loading follow requests", e)
            LoadResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while loading follow requests", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Account>): String? {
        return null
    }
}
