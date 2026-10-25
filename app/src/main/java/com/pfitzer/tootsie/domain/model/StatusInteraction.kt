package com.pfitzer.tootsie.domain.model

/**
 * The user's interaction state with a status, as last confirmed by the server.
 * Applied on top of lists that were loaded before the interaction happened.
 *
 * @param editedContent Content of an own status edited during this session
 */
data class StatusInteraction(
    val favourited: Boolean,
    val favouritesCount: Int,
    val reblogged: Boolean,
    val reblogsCount: Int,
    val bookmarked: Boolean,
    val pinned: Boolean,
    val editedContent: EditedContent? = null
)

/**
 * The parts of a status that change when it is edited.
 */
data class EditedContent(
    val content: String,
    val spoilerText: String,
    val sensitive: Boolean,
    val mediaAttachments: List<MediaAttachment>,
    val mentions: List<Mention>,
    val tags: List<Tag>,
    val emojis: List<CustomEmoji>,
    val poll: Poll?,
    val card: Card?,
    val language: String?,
    val editedAt: String?
)

/**
 * Snapshot of the editable content of this status.
 */
fun Status.toEditedContent() = EditedContent(
    content = content,
    spoilerText = spoilerText,
    sensitive = sensitive,
    mediaAttachments = mediaAttachments,
    mentions = mentions,
    tags = tags,
    emojis = emojis,
    poll = poll,
    card = card,
    language = language,
    editedAt = editedAt
)

/**
 * Snapshot of the interaction state of this status.
 */
fun Status.toInteraction() = StatusInteraction(
    favourited = favourited,
    favouritesCount = favouritesCount,
    reblogged = reblogged,
    reblogsCount = reblogsCount,
    bookmarked = bookmarked,
    pinned = pinned
)

/**
 * Whether this status, or the status it boosts, is in [deletedIds].
 */
fun Status.isDeletedIn(deletedIds: Set<String>): Boolean =
    id in deletedIds || (reblog != null && reblog.id in deletedIds)

/**
 * Apply known interaction overrides to this status and its boosted status.
 */
fun Status.withInteractions(overrides: Map<String, StatusInteraction>): Status {
    if (overrides.isEmpty()) return this
    val updatedReblog = reblog?.withInteractions(overrides)
    val interaction = overrides[id]
    if (interaction == null && updatedReblog === reblog) return this
    val base = if (updatedReblog === reblog) this else copy(reblog = updatedReblog)
    return interaction?.let {
        val withInteraction = base.copy(
            favourited = it.favourited,
            favouritesCount = it.favouritesCount,
            reblogged = it.reblogged,
            reblogsCount = it.reblogsCount,
            bookmarked = it.bookmarked,
            pinned = it.pinned
        )
        it.editedContent?.let { edit ->
            withInteraction.copy(
                content = edit.content,
                spoilerText = edit.spoilerText,
                sensitive = edit.sensitive,
                mediaAttachments = edit.mediaAttachments,
                mentions = edit.mentions,
                tags = edit.tags,
                emojis = edit.emojis,
                poll = edit.poll,
                card = edit.card,
                language = edit.language,
                editedAt = edit.editedAt
            )
        } ?: withInteraction
    } ?: base
}
