package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.AsyncRefresh
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Waits for the server job that fetches missing remote replies of a thread.
 *
 * Polls the job no more often than the server's `retry` interval allows and gives up after
 * [MAX_ATTEMPTS] polls or on errors (e.g. servers without the experimental endpoint).
 *
 * @return true if the job finished and fetched new replies, so the context should be reloaded
 */
class AwaitRemoteRepliesUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    suspend operator fun invoke(refresh: AsyncRefresh): Boolean {
        val intervalMillis = refresh.retrySeconds.coerceAtLeast(1) * 1000L
        repeat(MAX_ATTEMPTS) {
            delay(intervalMillis)
            when (val result = statusRepository.getAsyncRefresh(refresh.id)) {
                is Result.Success -> if (result.data.finished) {
                    // A finished job without a count may still have fetched something
                    return (result.data.resultCount ?: 1) > 0
                }
                is Result.Error -> return false
                is Result.Loading -> Unit
            }
        }
        return false
    }

    companion object {
        const val MAX_ATTEMPTS = 10
    }
}
