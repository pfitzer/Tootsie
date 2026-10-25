package com.pfitzer.tootsie.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ScheduleTimeTest {

    private val now = Instant.parse("2026-10-25T10:02:30Z")

    @Test
    fun `times at least 5 minutes ahead are valid`() {
        assertTrue(ScheduleTime.isValid(Instant.parse("2026-10-25T10:07:30Z"), now))
        assertFalse(ScheduleTime.isValid(Instant.parse("2026-10-25T10:07:29Z"), now))
        assertFalse(ScheduleTime.isValid(Instant.parse("2026-10-25T09:00:00Z"), now))
    }

    @Test
    fun `suggestion is one hour ahead, rounded up to 5 minutes`() {
        assertEquals(Instant.parse("2026-10-25T11:05:00Z"), ScheduleTime.suggestion(now))
        assertEquals(Instant.parse("2026-10-25T11:00:00Z"), ScheduleTime.suggestion(Instant.parse("2026-10-25T10:00:00Z")))
    }

    @Test
    fun `picker date and time are local`() {
        val berlin = ZoneId.of("Europe/Berlin")
        // 00:30 in Berlin is still the previous day in UTC
        val at = Instant.parse("2026-07-01T22:30:00Z")

        val dateMillis = ScheduleTime.toDatePickerMillis(at, berlin)

        assertEquals(Instant.parse("2026-07-02T00:00:00Z").toEpochMilli(), dateMillis)
        assertEquals(at, ScheduleTime.fromPicker(dateMillis, 0, 30, berlin))
    }
}
