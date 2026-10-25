package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.ScheduledStatusDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScheduledStatusMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `form values sent as strings are read`() {
        val dto = json.decodeFromString<ScheduledStatusDto>(
            """
            {
              "id": "3221",
              "scheduled_at": "2026-10-26T09:00:00.000Z",
              "params": {
                "text": "Hello",
                "spoiler_text": "",
                "visibility": "unlisted",
                "sensitive": "true",
                "language": "de",
                "in_reply_to_id": 103,
                "application_id": 596551,
                "poll": { "options": ["A", "B"], "expires_in": "3600", "multiple": "false", "hide_totals": true }
              },
              "media_attachments": []
            }
            """
        )

        val status = dto.toDomain()

        assertEquals(Instant.parse("2026-10-26T09:00:00Z"), status.scheduledAt)
        assertEquals("Hello", status.text)
        assertNull(status.spoilerText)
        assertEquals("unlisted", status.visibility)
        assertTrue(status.sensitive)
        assertEquals("103", status.inReplyToId)
        assertEquals(listOf("A", "B"), status.poll?.options)
        assertEquals(3600, status.poll?.expiresIn)
        assertFalse(status.poll!!.multiple)
        assertTrue(status.poll!!.hideTotals)
    }

    @Test
    fun `missing and null values have defaults`() {
        val dto = json.decodeFromString<ScheduledStatusDto>(
            """
            {
              "id": "1",
              "scheduled_at": "2026-10-26T09:00:00Z",
              "params": { "text": "Hi", "visibility": null, "in_reply_to_id": null, "poll": null }
            }
            """
        )

        val status = dto.toDomain()

        assertEquals("public", status.visibility)
        assertFalse(status.sensitive)
        assertNull(status.inReplyToId)
        assertNull(status.poll)
        assertTrue(status.mediaAttachments.isEmpty())
    }
}
