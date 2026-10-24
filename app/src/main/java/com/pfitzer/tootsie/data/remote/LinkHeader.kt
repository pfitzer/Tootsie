package com.pfitzer.tootsie.data.remote

/**
 * Pagination via the HTTP `Link` header, used by Mastodon endpoints whose pages are not
 * keyed by the IDs of the returned items (followed tags, mutes, blocks, …).
 */
object LinkHeader {

    private val NEXT_MAX_ID = Regex("""<[^>]*[?&]max_id=([^>&]*)[^>]*>;\s*rel="next"""")

    /**
     * The `max_id` of the next (older) page, or null if there is none.
     * Format: `<https://…?max_id=123>; rel="next", <https://…?min_id=456>; rel="prev"`
     */
    fun nextMaxId(header: String?): String? {
        if (header.isNullOrBlank()) return null
        return NEXT_MAX_ID.find(header)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
    }
}
