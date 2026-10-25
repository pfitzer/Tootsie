package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.local.entities.StatusEntity
import com.pfitzer.tootsie.data.remote.dto.*
import com.pfitzer.tootsie.domain.model.Quote
import com.pfitzer.tootsie.domain.model.QuotePermission
import com.pfitzer.tootsie.domain.model.QuoteState
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
        // The quote is shown as a card, so drop the "RE: <link>" fallback paragraph
        content = if (quote != null) content.withoutQuoteInline() else content,
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
        language = language,
        quote = quote?.let {
            Quote(
                state = QuoteState.fromApi(it.state),
                quotedStatus = it.quotedStatus?.toDomain(),
                quotedStatusId = it.quotedStatusId ?: it.quotedStatus?.id
            )
        },
        quotePermission = quoteApproval?.let { QuotePermission.fromApi(it.currentUser) }
    )
}

/**
 * Mastodon prepends `<p class="quote-inline">RE: <a …></a></p>` to quote posts for clients
 * without quote support.
 */
private val QUOTE_INLINE = Regex("""<p class="quote-inline">.*?</p>""", RegexOption.DOT_MATCHES_ALL)

internal fun String.withoutQuoteInline(): String = QUOTE_INLINE.replace(this, "")

/**
 * Maps StatusDto to StatusEntity for the timeline cache.
 */
fun StatusDto.toEntity(
    timelineType: String,
    ownerAccountId: String
): StatusEntity {
    // Interaction state belongs to the status that is shown, i.e. the boosted one
    val content = reblog ?: this
    return StatusEntity(
        id = id,
        timelineType = timelineType,
        ownerAccountId = ownerAccountId,
        contentId = content.id,
        statusJson = json.encodeToString(this),
        favourited = content.favourited,
        favouritesCount = content.favouritesCount,
        reblogged = content.reblogged,
        reblogsCount = content.reblogsCount,
        bookmarked = content.bookmarked,
        pinned = content.pinned
    )
}

/**
 * Maps StatusEntity (database) to Status (domain).
 * The interaction columns override the stored JSON, since they are updated in place.
 */
fun StatusEntity.toDomain(): Status {
    val status = json.decodeFromString<StatusDto>(statusJson).toDomain()
    val content = status.reblog ?: status
    val updatedContent = content.copy(
        favourited = favourited,
        favouritesCount = favouritesCount,
        reblogged = reblogged,
        reblogsCount = reblogsCount,
        bookmarked = bookmarked,
        pinned = pinned
    )
    return if (status.reblog != null) status.copy(reblog = updatedContent) else updatedContent
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

internal fun MediaAttachmentDto.toDomain(): MediaAttachment {
    return MediaAttachment(
        id = id,
        type = type.toMediaTypeEnum(),
        url = url,
        previewUrl = previewUrl ?: url, // Fall back to full URL if no preview available
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

fun PollDto.toDomain(): Poll {
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
