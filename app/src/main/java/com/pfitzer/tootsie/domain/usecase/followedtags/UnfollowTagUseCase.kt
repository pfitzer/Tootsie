package com.pfitzer.tootsie.domain.usecase.followedtags

import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.repository.FollowedTagsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unfollowing a hashtag.
 */
class UnfollowTagUseCase @Inject constructor(
    private val followedTagsRepository: FollowedTagsRepository
) {
    suspend operator fun invoke(tagName: String): Result<FollowedTag> {
        return followedTagsRepository.unfollowTag(tagName)
    }
}
