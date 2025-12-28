package com.pfitzer.tootsie.domain.model

/**
 * Domain model for a trending link.
 * Represents a link that has been shared more than others on the instance.
 */
data class TrendingLink(
    val url: String,
    val title: String,
    val description: String,
    val type: String,
    val authorName: String?,
    val authorUrl: String?,
    val providerName: String?,
    val providerUrl: String?,
    val html: String?,
    val width: Int?,
    val height: Int?,
    val image: String?,
    val embedUrl: String?,
    val blurhash: String?,
    val history: List<LinkHistory>
) {
    /**
     * Historical usage data for a trending link.
     */
    data class LinkHistory(
        val day: String,
        val accounts: Int,
        val uses: Int
    )

    /**
     * Total number of times this link has been used across all history.
     */
    val totalUses: Int
        get() = history.sumOf { it.uses }

    /**
     * Total number of unique accounts that have shared this link across all history.
     */
    val totalAccounts: Int
        get() = history.sumOf { it.accounts }
}
