package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.repository.ReportRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : ReportRepository {

    override suspend fun report(
        accountId: String,
        statusIds: List<String>?,
        comment: String?,
        forward: Boolean,
        category: String
    ): Result<Unit> {
        return try {
            mastodonApi.report(accountId, statusIds, comment, forward, category)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to submit report")
        }
    }
}
