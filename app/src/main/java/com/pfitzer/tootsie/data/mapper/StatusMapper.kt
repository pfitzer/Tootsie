package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.local.entities.StatusEntity
import com.pfitzer.tootsie.data.remote.dto.*
import com.pfitzer.tootsie.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Maps StatusDto (network) to Status (domain).
 */
fun StatusDto.toDomain(): Status {
    return Status(
        id = id,
        uri = uri,
        url = url,
        account = account.toDomain(),
        content = content,
        createdAt = createdAt,
        editedAt = editedAt,
        visibility = visibility.toVisibilityEnum(),
        sensitive = sensitive,
        spoilerText = spoilerText,
        mediaAttachments = mediaAttachments.map { it.toDomain() },
        mentions = mentions.map { it.toDomain() },
        tags = tags.map { it.toDomain() },
        emojis = emojis.map { it.toDomain() },
        reblogsCount = reblogsCount,
        favouritesCount = favouritesCount,
        repliesCount = repliesCount,
        poll = poll?.toDomain(),
        reblog = reblog?.toDomain(), // Recursive for reblogged posts
        inReplyToId = inReplyToId,
        inReplyToAccountId = inReplyToAccountId,
        reblogged = reblogged,
        favourited = favourited,
        bookmarked = bookmarked,
        pinned = pinned,
        card = card?.toDomain(),
        language = language
    )
}

/**
 * Maps StatusDto to StatusEntity for caching.
 */
fun StatusDto.toEntity(
    timelineType: String? = null,
    ownerAccountId: String? = null
): StatusEntity {
    return StatusEntity(
        id = id,
        accountId = account.id,
        uri = uri,
        url = url,
        content = content,
        createdAt = createdAt,
        editedAt = editedAt,
        visibility = visibility,
        sensitive = sensitive,
        spoilerText = spoilerText,
        reblogsCount = reblogsCount,
        favouritesCount = favouritesCount,
        repliesCount = repliesCount,
        inReplyToId = inReplyToId,
        inReplyToAccountId = inReplyToAccountId,
        reblogged = reblogged,
        favourited = favourited,
        bookmarked = bookmarked,
        pinned = pinned,
        language = language,
        accountJson = json.encodeToString(account),
        mediaAttachmentsJson = if (mediaAttachments.isNotEmpty()) json.encodeToString(mediaAttachments) else null,
        mentionsJson = if (mentions.isNotEmpty()) json.encodeToString(mentions) else null,
        tagsJson = if (tags.isNotEmpty()) json.encodeToString(tags) else null,
        emojisJson = if (emojis.isNotEmpty()) json.encodeToString(emojis) else null,
        pollJson = poll?.let { json.encodeToString(it) },
        reblogJson = reblog?.let { json.encodeToString(it) },
        cardJson = card?.let { json.encodeToString(it) },
        timelineType = timelineType,
        ownerAccountId = ownerAccountId
    )
}

/**
 * Maps StatusEntity (database) to Status (domain).
 */
fun StatusEntity.toDomain(): Status {
    val accountDto = json.decodeFromString<AccountDto>(accountJson)
    val mediaAttachmentsList = mediaAttachmentsJson?.let {
        json.decodeFromString<List<MediaAttachmentDto>>(it)
    } ?: emptyList()
    val mentionsList = mentionsJson?.let {
        json.decodeFromString<List<MentionDto>>(it)
    } ?: emptyList()
    val tagsList = tagsJson?.let {
        json.decodeFromString<List<TagDto>>(it)
    } ?: emptyList()
    val emojisList = emojisJson?.let {
        json.decodeFromString<List<CustomEmojiDto>>(it)
    } ?: emptyList()
    val pollData = pollJson?.let {
        json.decodeFromString<PollDto>(it)
    }
    val reblogData = reblogJson?.let {
        json.decodeFromString<StatusDto>(it)
    }
    val cardData = cardJson?.let {
        json.decodeFromString<CardDto>(it)
    }

    return Status(
        id = id,
        uri = uri,
        url = url,
        account = accountDto.toDomain(),
        content = content,
        createdAt = createdAt,
        editedAt = editedAt,
        visibility = visibility.toVisibilityEnum(),
        sensitive = sensitive,
        spoilerText = spoilerText,
        mediaAttachments = mediaAttachmentsList.map { it.toDomain() },
        mentions = mentionsList.map { it.toDomain() },
        tags = tagsList.map { it.toDomain() },
        emojis = emojisList.map { it.toDomain() },
        reblogsCount = reblogsCount,
        favouritesCount = favouritesCount,
        repliesCount = repliesCount,
        poll = pollData?.toDomain(),
        reblog = reblogData?.toDomain(),
        inReplyToId = inReplyToId,
        inReplyToAccountId = inReplyToAccountId,
        reblogged = reblogged,
        favourited = favourited,
        bookmarked = bookmarked,
        pinned = pinned,
        card = cardData?.toDomain(),
        language = language
    )
}

// Helper mappers for nested types

private fun String.toVisibilityEnum(): StatusVisibility {
    return when (this.lowercase()) {
        "public" -> StatusVisibility.PUBLIC
        "unlisted" -> StatusVisibility.UNLISTED
        "private" -> StatusVisibility.PRIVATE
        "direct" -> StatusVisibility.DIRECT
        else -> StatusVisibility.PUBLIC
    }
}

private fun MediaAttachmentDto.toDomain(): MediaAttachment {
    return MediaAttachment(
        id = id,
        type = type.toMediaTypeEnum(),
        url = url,
        previewUrl = previewUrl,
        description = description,
        blurhash = blurhash,
        meta = meta?.original?.let {
            MediaMeta(
                width = it.width,
                height = it.height,
                duration = it.duration,
                aspect = it.aspect
            )
        }
    )
}

private fun String.toMediaTypeEnum(): MediaType {
    return when (this.lowercase()) {
        "image" -> MediaType.IMAGE
        "video" -> MediaType.VIDEO
        "gifv" -> MediaType.GIFV
        "audio" -> MediaType.AUDIO
        else -> MediaType.UNKNOWN
    }
}

private fun MentionDto.toDomain(): Mention {
    return Mention(
        id = id,
        username = username,
        acct = acct,
        url = url
    )
}

private fun TagDto.toDomain(): Tag {
    return Tag(
        name = name,
        url = url
    )
}

private fun PollDto.toDomain(): Poll {
    return Poll(
        id = id,
        expiresAt = expiresAt,
        expired = expired,
        multiple = multiple,
        votesCount = votesCount,
        votersCount = votersCount,
        voted = voted,
        ownVotes = ownVotes,
        options = options.map { it.toDomain() }
    )
}

private fun PollOptionDto.toDomain(): PollOption {
    return PollOption(
        title = title,
        votesCount = votesCount
    )
}

private fun CardDto.toDomain(): Card {
    return Card(
        url = url,
        title = title,
        description = description,
        type = type.toCardTypeEnum(),
        authorName = authorName,
        authorUrl = authorUrl,
        providerName = providerName,
        providerUrl = providerUrl,
        html = html,
        width = width,
        height = height,
        image = image,
        embedUrl = embedUrl
    )
}

private fun String.toCardTypeEnum(): CardType {
    return when (this.lowercase()) {
        "link" -> CardType.LINK
        "photo" -> CardType.PHOTO
        "video" -> CardType.VIDEO
        "rich" -> CardType.RICH
        else -> CardType.LINK
    }
}
