package com.pfitzer.tootsie.domain.usecase.conversation

import com.pfitzer.tootsie.domain.model.Conversation
import com.pfitzer.tootsie.domain.repository.ConversationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case to mark a conversation as read.
 */
class MarkConversationAsReadUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(id: String): Result<Conversation> {
        return conversationRepository.markConversationAsRead(id)
    }
}
