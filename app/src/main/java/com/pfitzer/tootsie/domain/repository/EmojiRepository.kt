package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.util.Result

/**
 * Repository for managing custom emojis.
 */
interface EmojiRepository {
    /**
     * Get all custom emojis available on the current instance.
     */
    suspend fun getCustomEmojis(): Result<List<CustomEmoji>>
}
