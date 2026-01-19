package com.pfitzer.tootsie.domain.usecase.conversation

import com.pfitzer.tootsie.domain.repository.ConversationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case to delete a conversation.
 */
class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return conversationRepository.deleteConversation(id)
    }
}
