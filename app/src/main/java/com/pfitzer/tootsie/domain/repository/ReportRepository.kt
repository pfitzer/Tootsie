package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.util.Result

interface ReportRepository {
    suspend fun report(
        accountId: String,
        statusIds: List<String>? = null,
        comment: String? = null,
        forward: Boolean = false,
        category: String = "other"
    ): Result<Unit>
}
