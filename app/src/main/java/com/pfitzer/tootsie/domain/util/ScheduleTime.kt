package com.pfitzer.tootsie.domain.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Times for scheduled posts.
 */
object ScheduleTime {

    /** Mastodon only accepts times at least this far ahead. */
    val MIN_LEAD: Duration = Duration.ofMinutes(5)

    /** Whether [at] can be used as publication time at [now]. */
    fun isValid(at: Instant, now: Instant): Boolean = !at.isBefore(now.plus(MIN_LEAD))

    /** Suggested time for a new schedule: one hour ahead, rounded up to 5 minutes. */
    fun suggestion(now: Instant): Instant {
        val inOneHour = now.plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MINUTES)
        val minute = inOneHour.atZone(ZoneOffset.UTC).minute
        return inOneHour.plus(Duration.ofMinutes(((5 - minute % 5) % 5).toLong()))
    }

    /**
     * The local date of [at] as UTC midnight in milliseconds, the format of the Material
     * date picker.
     */
    fun toDatePickerMillis(at: Instant, zone: ZoneId): Long =
        at.atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Combines a date picker date with a local time of day. */
    fun fromPicker(dateMillis: Long, hour: Int, minute: Int, zone: ZoneId): Instant =
        Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
}
