package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.util.Result

/**
 * Timelines whose reading position Mastodon stores as a marker.
 */
enum class MarkerTimeline(val apiName: String) {
    HOME("home"),
    NOTIFICATIONS("notifications")
}

/**
 * Reading positions (markers), shared with the web interface and other clients.
 */
interface MarkerRepository {

    /**
     * ID of the last read status or notification of [timeline], null if none is stored.
     */
    suspend fun getMarker(timeline: MarkerTimeline): Result<String?>

    /**
     * Store the last read status or notification of [timeline].
     */
    suspend fun saveMarker(timeline: MarkerTimeline, lastReadId: String): Result<Unit>
}
