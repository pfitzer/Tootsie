package com.pfitzer.tootsie.data.remote

import com.pfitzer.tootsie.domain.model.AsyncRefresh

/**
 * Parser for the experimental `Mastodon-Async-Refresh` response header:
 * `id="<string>", retry=<int>, result_count=<int>` (result_count is optional).
 *
 * See https://docs.joinmastodon.org/methods/async_refreshes/
 */
object AsyncRefreshHeader {

    const val NAME = "Mastodon-Async-Refresh"

    private val PARAMETER = Regex("""(\w+)\s*=\s*(?:"([^"]*)"|([^,\s]+))""")

    /**
     * Returns null if the header is missing or has no id.
     */
    fun parse(value: String?): AsyncRefresh? {
        if (value.isNullOrBlank()) return null
        val parameters = PARAMETER.findAll(value).associate { match ->
            match.groupValues[1].lowercase() to (match.groups[2]?.value ?: match.groupValues[3])
        }
        val id = parameters["id"]?.takeIf { it.isNotBlank() } ?: return null
        return AsyncRefresh(
            id = id,
            retrySeconds = parameters["retry"]?.toIntOrNull()?.coerceAtLeast(1) ?: DEFAULT_RETRY_SECONDS,
            resultCount = parameters["result_count"]?.toIntOrNull()
        )
    }

    private const val DEFAULT_RETRY_SECONDS = 3
}
