package com.pfitzer.tootsie.domain.usecase.followedtags

import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.repository.FollowedTagsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting followed hashtags.
 */
class GetFollowedTagsUseCase @Inject constructor(
    private val followedTagsRepository: FollowedTagsRepository
) {
    suspend operator fun invoke(): Result<List<FollowedTag>> {
        return followedTagsRepository.getFollowedTags()
    }
}
