package com.pfitzer.tootsie.domain.util

/**
 * Comparison of Mastodon IDs. They are numeric strings that grow over time; comparing the
 * length first orders them numerically without parsing (they can exceed Long on forks).
 */
object MastodonIds {

    /** Whether [id] is newer than [other]. */
    fun isNewer(id: String, other: String): Boolean =
        id.length > other.length || (id.length == other.length && id > other)
}
