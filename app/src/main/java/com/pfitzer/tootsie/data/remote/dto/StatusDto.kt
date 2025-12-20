package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for Mastodon Status API responses.
 */
@Serializable
data class StatusDto(
    @SerialName("id") val id: String,
    @SerialName("uri") val uri: String,
    @SerialName("url") val url: String? = null,
    @SerialName("account") val account: AccountDto,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("visibility") val visibility: String,
    @SerialName("sensitive") val sensitive: Boolean,
    @SerialName("spoiler_text") val spoilerText: String,
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachmentDto> = emptyList(),
    @SerialName("mentions") val mentions: List<MentionDto> = emptyList(),
    @SerialName("tags") val tags: List<TagDto> = emptyList(),
    @SerialName("emojis") val emojis: List<CustomEmojiDto> = emptyList(),
    @SerialName("reblogs_count") val reblogsCount: Int,
    @SerialName("favourites_count") val favouritesCount: Int,
    @SerialName("replies_count") val repliesCount: Int,
    @SerialName("poll") val poll: PollDto? = null,
    @SerialName("reblog") val reblog: StatusDto? = null,
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    @SerialName("in_reply_to_account_id") val inReplyToAccountId: String? = null,
    @SerialName("reblogged") val reblogged: Boolean = false,
    @SerialName("favourited") val favourited: Boolean = false,
    @SerialName("bookmarked") val bookmarked: Boolean = false,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("card") val card: CardDto? = null,
    @SerialName("language") val language: String? = null
)

@Serializable
data class MediaAttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("url") val url: String,
    @SerialName("preview_url") val previewUrl: String,
    @SerialName("description") val description: String? = null,
    @SerialName("blurhash") val blurhash: String? = null,
    @SerialName("meta") val meta: MediaMetaDto? = null
)

@Serializable
data class MediaMetaDto(
    @SerialName("original") val original: MediaMetaDetailsDto? = null
)

@Serializable
data class MediaMetaDetailsDto(
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("duration") val duration: Float? = null,
    @SerialName("aspect") val aspect: Float? = null
)

@Serializable
data class MentionDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("acct") val acct: String,
    @SerialName("url") val url: String
)

@Serializable
data class TagDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String
)

@Serializable
data class PollDto(
    @SerialName("id") val id: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("expired") val expired: Boolean,
    @SerialName("multiple") val multiple: Boolean,
    @SerialName("votes_count") val votesCount: Int,
    @SerialName("voters_count") val votersCount: Int? = null,
    @SerialName("voted") val voted: Boolean = false,
    @SerialName("own_votes") val ownVotes: List<Int> = emptyList(),
    @SerialName("options") val options: List<PollOptionDto>
)

@Serializable
data class PollOptionDto(
    @SerialName("title") val title: String,
    @SerialName("votes_count") val votesCount: Int? = null
)

@Serializable
data class CardDto(
    @SerialName("url") val url: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("type") val type: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_url") val authorUrl: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("provider_url") val providerUrl: String? = null,
    @SerialName("html") val html: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("embed_url") val embedUrl: String? = null
)
