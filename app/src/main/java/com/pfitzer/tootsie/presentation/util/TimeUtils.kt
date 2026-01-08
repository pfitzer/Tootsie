package com.pfitzer.tootsie.presentation.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Formats an ISO 8601 timestamp as a relative time string.
 * Examples: "2m", "5h", "3d", "Jan 15"
 *
 * @param isoTimestamp ISO 8601 timestamp string
 * @return Formatted relative time string
 *
 * @deprecated Use LocaleAwareDateFormatter.formatRelativeTime() instead for proper i18n support.
 * This function uses hardcoded date patterns and doesn't respect user locale.
 */
@Deprecated(
    message = "Use LocaleAwareDateFormatter.formatRelativeTime(context, isoTimestamp) for locale-aware formatting",
    replaceWith = ReplaceWith(
        "LocaleAwareDateFormatter.formatRelativeTime(context, isoTimestamp)",
        "com.pfitzer.tootsie.presentation.util.LocaleAwareDateFormatter"
    ),
    level = DeprecationLevel.WARNING
)
fun formatRelativeTime(isoTimestamp: String): String {
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
                // Format as date for older posts
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

/**
 * Formats an ISO 8601 timestamp as an absolute date/time.
 *
 * @param isoTimestamp ISO 8601 timestamp string
 * @return Formatted date/time string
 *
 * @deprecated Use LocaleAwareDateFormatter.formatAbsoluteTime() instead for proper i18n support.
 * This function uses hardcoded date patterns and doesn't respect user locale.
 */
@Deprecated(
    message = "Use LocaleAwareDateFormatter.formatAbsoluteTime(context, isoTimestamp) for locale-aware formatting",
    replaceWith = ReplaceWith(
        "LocaleAwareDateFormatter.formatAbsoluteTime(context, isoTimestamp)",
        "com.pfitzer.tootsie.presentation.util.LocaleAwareDateFormatter"
    ),
    level = DeprecationLevel.WARNING
)
fun formatAbsoluteTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoTimestamp
    }
}
