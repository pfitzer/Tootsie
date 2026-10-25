package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.remote.LinkHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "BlockedDomainsPaging"

/**
 * PagingSource for blocked domains.
 * Uses the Link header for pagination, as the API pages by internal block IDs.
 */
class BlockedDomainsPagingSource(
    private val mastodonApi: MastodonApiService
) : PagingSource<String, String>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, String> {
        return try {
            val response = mastodonApi.getBlockedDomains(
                maxId = params.key,
                limit = params.loadSize
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                return LoadResult.Error(HttpException(response))
            }

            LoadResult.Page(
                data = response.body() ?: emptyList(),
                prevKey = null,
                nextKey = LinkHeader.nextMaxId(response.headers()["Link"])
            )
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException: ${e.code()} - ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, String>): String? = null
}
