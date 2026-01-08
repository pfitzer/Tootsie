package com.pfitzer.tootsie.presentation.util

import android.content.Context
import com.pfitzer.tootsie.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formats dates and times with locale awareness.
 * Uses string resources for patterns to support localization.
 *
 * This utility replaces the deprecated TimeUtils.kt and provides:
 * - Locale-aware date formatting
 * - Unified timestamp formatting (removes DraftsScreen duplication)
 * - Support for both ISO 8601 strings and Long milliseconds
 */
object LocaleAwareDateFormatter {

    /**
     * Format relative time from ISO 8601 timestamp (e.g., "2m", "5h", "Jan 15")
     * Uses short abbreviations for time units (minimal translation needed)
     *
     * @param context Android context for accessing string resources
     * @param isoTimestamp ISO 8601 timestamp string from API (e.g., "2024-01-08T12:34:56.789Z")
     * @return Formatted relative time string
     */
    fun formatRelativeTime(context: Context, isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val now = Instant.now()
            val diff = ChronoUnit.SECONDS.between(instant, now)

            when {
                diff < 60 -> "${diff}s"
                diff < 3600 -> "${diff / 60}m"
                diff < 86400 -> "${diff / 3600}h"
                diff < 604800 -> "${diff / 86400}d"
                else -> {
                    // Use locale-aware pattern from string resources
                    val pattern = context.getString(R.string.date_format_short)
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Format absolute date/time from ISO 8601 timestamp
     * (e.g., "Jan 15, 2024 at 3:45 PM" for English, "15. Jan 2024 um 15:45 Uhr" for German)
     *
     * @param context Android context for accessing string resources
     * @param isoTimestamp ISO 8601 timestamp string from API
     * @return Formatted absolute date/time string
     */
    fun formatAbsoluteTime(context: Context, isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val pattern = context.getString(R.string.date_format_long)
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            isoTimestamp
        }
    }

    /**
     * Format timestamp for drafts from Long milliseconds
     * (e.g., "Just now", "5m ago", "3h ago", "Jan 15")
     *
     * This replaces the duplicate formatTimestamp() function in DraftsScreen.kt
     *
     * @param context Android context for accessing string resources
     * @param timestamp Milliseconds since epoch (from System.currentTimeMillis())
     * @return Formatted relative time string with "ago" suffix
     */
    fun formatDraftTimestamp(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> context.getString(R.string.time_just_now)
            diff < 3600_000 -> context.getString(R.string.time_minutes_ago, diff / 60_000)
            diff < 86400_000 -> context.getString(R.string.time_hours_ago, diff / 3600_000)
            diff < 604800_000 -> context.getString(R.string.time_days_ago, diff / 86400_000)
            else -> {
                // Use locale-aware pattern for older drafts
                val pattern = context.getString(R.string.date_format_short)
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                formatter.format(Instant.ofEpochMilli(timestamp))
            }
        }
    }
}
