package com.pfitzer.tootsie.domain.model

/**
 * A background job on the server, e.g. fetching remote replies of a thread
 * (Mastodon >= 4.5, announced via the `Mastodon-Async-Refresh` response header).
 */
data class AsyncRefresh(
    val id: String,
    /** Minimum number of seconds to wait before polling the job */
    val retrySeconds: Int,
    /** Results already fetched, if the server reports it */
    val resultCount: Int? = null
)

/**
 * Current state of an [AsyncRefresh] job.
 */
data class AsyncRefreshProgress(
    val finished: Boolean,
    /** Number of results fetched or generated; 0 means nothing new */
    val resultCount: Int?
)
