package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.LinkHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.domain.model.FollowListEntry
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val TAG = "FollowListPaging"

/**
 * PagingSource for the followers or following list of an account.
 * Pages via the Link header (the API pages by internal follow IDs) and loads the user's
 * relationships to the accounts of each page in one request.
 *
 * @param fetchPage Loads a page: (maxId, limit) -> accounts
 */
class FollowListPagingSource(
    private val mastodonApi: MastodonApiService,
    private val fetchPage: suspend (maxId: String?, limit: Int) -> Response<List<AccountDto>>
) : PagingSource<String, FollowListEntry>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, FollowListEntry> {
        return try {
            val response = fetchPage(params.key, params.loadSize)
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                return LoadResult.Error(HttpException(response))
            }

            val accounts = response.body().orEmpty().map { it.toDomain() }
            // Only needed for the follow buttons, so the list is shown without them on failure
            val relationships = if (accounts.isEmpty()) {
                emptyMap()
            } else {
                runCatching { mastodonApi.getRelationships(accounts.map { it.id }) }
                    .getOrDefault(emptyList())
                    .associate { it.id to it.toDomain() }
            }

            LoadResult.Page(
                data = accounts.map { FollowListEntry(it, relationships[it.id]) },
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

    override fun getRefreshKey(state: PagingState<String, FollowListEntry>): String? = null
}
