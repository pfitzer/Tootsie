package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "MutedAccountsPaging"

/**
 * PagingSource for Muted Accounts.
 * Fetches accounts that the user has muted.
 * Uses Link header for pagination as the API uses internal relationship IDs.
 */
class MutedAccountsPagingSource(
    private val mastodonApi: MastodonApiService
) : PagingSource<String, Account>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Account> {
        return try {
            val maxId = params.key
            Log.d(TAG, "load() called - maxId=$maxId, loadSize=${params.loadSize}")

            val response = mastodonApi.getMutedAccounts(
                maxId = maxId,
                limit = params.loadSize
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                return LoadResult.Error(HttpException(response))
            }

            val accountDtos = response.body() ?: emptyList()
            Log.d(TAG, "API returned ${accountDtos.size} muted accounts")

            val accounts = accountDtos.map { it.toDomain() }

            // Extract next page ID from Link header
            val linkHeader = response.headers()["Link"]
            val nextKey = parseLinkHeader(linkHeader)

            Log.d(TAG, "Returning ${accounts.size} accounts, nextKey=$nextKey (from Link header)")
            LoadResult.Page(
                data = accounts,
                prevKey = null,
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

    /**
     * Parse the Link header to extract the max_id for the next page.
     * Link header format: <url?max_id=123>; rel="next", <url?since_id=456>; rel="prev"
     */
    private fun parseLinkHeader(linkHeader: String?): String? {
        if (linkHeader.isNullOrBlank()) return null

        // Find the "next" link
        val nextPattern = """<[^>]*[?&]max_id=([^>&]*)>;\s*rel="next"""".toRegex()
        val match = nextPattern.find(linkHeader)
        return match?.groupValues?.get(1)
    }

    override fun getRefreshKey(state: PagingState<String, Account>): String? {
        return null
    }
}
