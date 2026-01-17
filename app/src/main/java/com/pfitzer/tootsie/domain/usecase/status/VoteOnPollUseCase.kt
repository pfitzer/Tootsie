package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Poll
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for voting on a poll.
 */
class VoteOnPollUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Votes on a poll with the given ID and choices.
     *
     * @param pollId The ID of the poll to vote on
     * @param choices List of option indices to vote for
     * @return Result containing the updated Poll
     */
    suspend operator fun invoke(pollId: String, choices: List<Int>): Result<Poll> {
        return statusRepository.voteOnPoll(pollId, choices)
    }
}
