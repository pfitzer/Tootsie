package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.toDomain
import com.pfitzer.tootsie.domain.model.Conversation
import com.pfitzer.tootsie.domain.repository.ConversationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ConversationRepository].
 */
@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : ConversationRepository {

    override suspend fun getConversations(
        maxId: String?,
        sinceId: String?,
        limit: Int
    ): Result<List<Conversation>> {
        return try {
            val response = mastodonApi.getConversations(maxId = maxId, sinceId = sinceId, limit = limit)
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }

    override suspend fun deleteConversation(id: String): Result<Unit> {
        return try {
            mastodonApi.deleteConversation(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }

    override suspend fun markConversationAsRead(id: String): Result<Conversation> {
        return try {
            val response = mastodonApi.markConversationAsRead(id)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
