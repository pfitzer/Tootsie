package com.pfitzer.tootsie.data.remote.dto

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.domain.model.Conversation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for Conversation from Mastodon API.
 */
@Serializable
data class ConversationDto(
    @SerialName("id") val id: String,
    @SerialName("unread") val unread: Boolean,
    @SerialName("accounts") val accounts: List<AccountDto>,
    @SerialName("last_status") val lastStatus: StatusDto? = null
)

/**
 * Maps ConversationDto to domain model Conversation.
 */
fun ConversationDto.toDomain(): Conversation {
    return Conversation(
        id = id,
        unread = unread,
        accounts = accounts.map { it.toDomain() },
        lastStatus = lastStatus?.toDomain()
    )
}
