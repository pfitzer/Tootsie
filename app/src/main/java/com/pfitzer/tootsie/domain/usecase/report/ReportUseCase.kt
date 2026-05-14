package com.pfitzer.tootsie.domain.usecase.report

import com.pfitzer.tootsie.domain.repository.ReportRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

class ReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(
        accountId: String,
        statusIds: List<String>? = null,
        comment: String? = null,
        forward: Boolean = false,
        category: String = "other"
    ): Result<Unit> = reportRepository.report(accountId, statusIds, comment, forward, category)
}
