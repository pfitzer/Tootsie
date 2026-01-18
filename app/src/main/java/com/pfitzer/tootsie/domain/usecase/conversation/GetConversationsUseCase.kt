package com.pfitzer.tootsie.domain.usecase.conversation

import com.pfitzer.tootsie.domain.model.Conversation
import com.pfitzer.tootsie.domain.repository.ConversationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case to fetch participated conversations.
 */
class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        maxId: String? = null,
        sinceId: String? = null,
        limit: Int = 20
    ): Result<List<Conversation>> {
        return conversationRepository.getConversations(maxId, sinceId, limit)
    }
}
