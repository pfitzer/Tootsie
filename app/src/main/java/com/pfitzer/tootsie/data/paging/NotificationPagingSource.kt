package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Notification
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "NotificationPaging"

/**
 * PagingSource for Notifications.
 * Fetches notifications from the Mastodon API with pagination.
 */
class NotificationPagingSource(
    private val mastodonApi: MastodonApiService,
    private val excludeTypes: List<String>? = null,
    private val accountId: String? = null
) : PagingSource<String, Notification>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Notification> {
        return try {
            val maxId = params.key // For pagination, load older notifications
            Log.d(TAG, "load() called - maxId=$maxId, loadSize=${params.loadSize}")

            // Fetch from API
            val notificationDtos = mastodonApi.getNotifications(
                maxId = maxId,
                limit = params.loadSize,
                excludeTypes = excludeTypes,
                accountId = accountId
            )

            Log.d(TAG, "API returned ${notificationDtos.size} notifications")

            // Convert to domain models
            val notifications = notificationDtos.map { it.toDomain() }

            // Determine next page key (ID of oldest notification)
            val nextKey = if (notifications.isEmpty()) {
                null
            } else {
                notifications.last().id
            }

            Log.d(TAG, "Returning ${notifications.size} notifications, nextKey=$nextKey")
            LoadResult.Page(
                data = notifications,
                prevKey = null, // Only load older notifications (forward pagination)
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

    override fun getRefreshKey(state: PagingState<String, Notification>): String? {
        // Return null to always start from the beginning on refresh
        return null
    }
}
