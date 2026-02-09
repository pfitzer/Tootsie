package com.pfitzer.tootsie.domain.usecase.account

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting statuses from an account (user timeline).
 */
class GetAccountStatusesUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Get account's timeline.
     *
     * @param accountId Account ID
     * @param excludeReplies Whether to exclude replies
     * @param excludeReblogs Whether to exclude boosts
     * @param onlyMedia Whether to only show posts with media
     * @param pinned Whether to only show pinned posts
     * @return Flow of paged statuses
     */
    operator fun invoke(
        accountId: String,
        excludeReplies: Boolean = false,
        excludeReblogs: Boolean = false,
        onlyMedia: Boolean = false,
        pinned: Boolean = false
    ): Flow<PagingData<Status>> {
        return accountRepository.getAccountStatuses(
            accountId = accountId,
            excludeReplies = excludeReplies,
            excludeReblogs = excludeReblogs,
            onlyMedia = onlyMedia,
            pinned = pinned
        )
    }
}
