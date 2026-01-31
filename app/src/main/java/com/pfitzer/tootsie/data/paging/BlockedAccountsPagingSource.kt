package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "BlockedAccountsPaging"

/**
 * PagingSource for Blocked Accounts.
 * Fetches accounts that the user has blocked.
 */
class BlockedAccountsPagingSource(
    private val mastodonApi: MastodonApiService
) : PagingSource<String, Account>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Account> {
        return try {
            val maxId = params.key
            Log.d(TAG, "load() called - maxId=$maxId, loadSize=${params.loadSize}")

            val accountDtos = mastodonApi.getBlockedAccounts(
                maxId = maxId,
                limit = params.loadSize
            )

            Log.d(TAG, "API returned ${accountDtos.size} blocked accounts")

            val accounts = accountDtos.map { it.toDomain() }

            val nextKey = if (accounts.isEmpty()) {
                null
            } else {
                accounts.last().id
            }

            Log.d(TAG, "Returning ${accounts.size} accounts, nextKey=$nextKey")
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

    override fun getRefreshKey(state: PagingState<String, Account>): String? {
        return null
    }
}
