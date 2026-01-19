package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Conversation
import com.pfitzer.tootsie.util.Result

/**
 * Repository for participating in direct message conversations.
 */
interface ConversationRepository {

    /**
     * Get participated conversations.
     *
     * @param maxId Return results older than this ID
     * @param sinceId Return results newer than this ID
     * @param limit Maximum number of results
     * @return List of conversations
     */
    suspend fun getConversations(
        maxId: String? = null,
        sinceId: String? = null,
        limit: Int = 20
    ): Result<List<Conversation>>

    /**
     * Remove a conversation.
     *
     * @param id Conversation ID
     */
    suspend fun deleteConversation(id: String): Result<Unit>

    /**
     * Mark a conversation as read.
     *
     * @param id Conversation ID
     * @return Updated conversation
     */
    suspend fun markConversationAsRead(id: String): Result<Conversation>
}
