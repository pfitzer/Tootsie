package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.paging.NotificationPagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository.
 * Handles notification operations via Mastodon API.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : NotificationRepository {

    override fun getNotifications(
        excludeTypes: List<String>?,
        accountId: String?
    ): Flow<PagingData<Notification>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                NotificationPagingSource(
                    mastodonApi = mastodonApi,
                    excludeTypes = excludeTypes,
                    accountId = accountId
                )
            }
        ).flow
    }

    override suspend fun dismissNotification(notificationId: String): Result<Unit> {
        return try {
            mastodonApi.dismissNotification(notificationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to dismiss notification")
        }
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            mastodonApi.clearAllNotifications()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to clear notifications")
        }
    }
}
