package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing search results from Mastodon API.
 * Contains results for accounts, statuses, and hashtags.
 */
data class SearchResults(
    val accounts: List<Account> = emptyList(),
    val statuses: List<Status> = emptyList(),
    val hashtags: List<Hashtag> = emptyList()
) {
    /**
     * Check if search results are empty.
     */
    val isEmpty: Boolean
        get() = accounts.isEmpty() && statuses.isEmpty() && hashtags.isEmpty()

    /**
     * Get total count of all results.
     */
    val totalCount: Int
        get() = accounts.size + statuses.size + hashtags.size
}

/**
 * Domain model representing a Mastodon hashtag with usage statistics.
 */
data class Hashtag(
    val name: String, // Without the # prefix
    val url: String, // Tag URL
    val history: List<HashtagHistory>? = null
) {
    /**
     * Get total uses across all history days.
     */
    val totalUses: Int
        get() = history?.sumOf { it.uses } ?: 0
}

/**
 * Hashtag usage history for a specific day.
 */
data class HashtagHistory(
    val day: String, // Unix timestamp
    val uses: Int, // Number of uses
    val accounts: Int // Number of unique accounts using this tag
)
