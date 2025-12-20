package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.model.Field

/**
 * Maps AccountDto (network) to Account (domain).
 */
fun AccountDto.toDomain(): Account {
    return Account(
        id = id,
        username = username,
        acct = acct,
        displayName = displayName,
        note = note,
        url = url,
        avatar = avatar,
        header = header,
        locked = locked,
        followersCount = followersCount,
        followingCount = followingCount,
        statusesCount = statusesCount,
        createdAt = createdAt,
        bot = bot,
        emojis = emojis.map { it.toDomain() },
        fields = fields.map { it.toDomain() }
    )
}

/**
 * Maps AccountEntity (database) to Account (domain).
 */
fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        username = username,
        acct = acct,
        displayName = displayName,
        note = note,
        url = url,
        avatar = avatar,
        header = header,
        locked = locked,
        followersCount = followersCount,
        followingCount = followingCount,
        statusesCount = statusesCount,
        createdAt = createdAt,
        bot = bot,
        emojis = emptyList(), // Simplified for entity
        fields = emptyList() // Simplified for entity
    )
}

/**
 * Maps AccountDto to AccountEntity for caching.
 */
fun AccountDto.toEntity(
    accessToken: String? = null,
    instanceUrl: String? = null,
    isActive: Boolean = false
): AccountEntity {
    return AccountEntity(
        id = id,
        username = username,
        acct = acct,
        displayName = displayName,
        note = note,
        url = url,
        avatar = avatar,
        header = header,
        locked = locked,
        followersCount = followersCount,
        followingCount = followingCount,
        statusesCount = statusesCount,
        createdAt = createdAt,
        bot = bot,
        accessToken = accessToken,
        instanceUrl = instanceUrl,
        isActive = isActive,
        lastSyncedAt = System.currentTimeMillis()
    )
}

/**
 * Maps CustomEmojiDto to CustomEmoji (domain).
 */
private fun com.pfitzer.tootsie.data.remote.dto.CustomEmojiDto.toDomain(): CustomEmoji {
    return CustomEmoji(
        shortcode = shortcode,
        url = url,
        staticUrl = staticUrl,
        visibleInPicker = visibleInPicker
    )
}

/**
 * Maps FieldDto to Field (domain).
 */
private fun com.pfitzer.tootsie.data.remote.dto.FieldDto.toDomain(): Field {
    return Field(
        name = name,
        value = value,
        verifiedAt = verifiedAt
    )
}
