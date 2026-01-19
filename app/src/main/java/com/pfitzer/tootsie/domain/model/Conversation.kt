package com.pfitzer.tootsie.domain.model

/**
 * Domain model for a conversation (direct message thread).
 */
data class Conversation(
    val id: String,
    val unread: Boolean,
    val accounts: List<Account>,
    val lastStatus: Status?
)
