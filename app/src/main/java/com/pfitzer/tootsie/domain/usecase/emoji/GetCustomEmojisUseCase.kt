package com.pfitzer.tootsie.domain.usecase.emoji

import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.repository.EmojiRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting custom emojis available on the instance.
 */
class GetCustomEmojisUseCase @Inject constructor(
    private val emojiRepository: EmojiRepository
) {
    suspend operator fun invoke(): Result<List<CustomEmoji>> {
        return emojiRepository.getCustomEmojis()
    }
}
