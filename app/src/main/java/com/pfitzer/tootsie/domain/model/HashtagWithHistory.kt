package com.pfitzer.tootsie.domain.model

import java.util.Locale

/**
 * A hashtag with usage history (followed or trending), sortable by name or activity.
 */
interface HashtagWithHistory {
    /** Tag name without the # prefix. */
    val name: String

    /** Posts with this tag in the history (the last 7 days). */
    val totalUses: Int
}

/** Sorted by name, case-insensitive. */
fun <T : HashtagWithHistory> List<T>.sortedByTagName(): List<T> = sortedWith(tagNameOrder())

/** Sorted by posts in the last 7 days, most active first; ties by name. */
fun <T : HashtagWithHistory> List<T>.sortedByTagActivity(): List<T> =
    sortedWith(compareByDescending<T> { it.totalUses }.then(tagNameOrder()))

private fun <T : HashtagWithHistory> tagNameOrder(): Comparator<T> = compareBy { it.name.lowercase(Locale.ROOT) }
