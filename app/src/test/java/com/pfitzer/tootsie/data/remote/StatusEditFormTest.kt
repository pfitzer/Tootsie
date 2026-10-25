package com.pfitzer.tootsie.data.remote

import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.model.PollConfig
import okhttp3.FormBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StatusEditFormTest {

    @Test
    fun `alt text fields of one attachment stay together`() {
        val form = StatusEditForm.build(
            NewStatus(content = "Hi", mediaIds = listOf("1", "2", "3"), sensitive = true, language = "de"),
            descriptions = mapOf("1" to "A cat", "3" to "A dog")
        )

        assertEquals(
            listOf(
                "status" to "Hi",
                "spoiler_text" to "",
                "sensitive" to "true",
                "language" to "de",
                "media_ids[]" to "1",
                "media_ids[]" to "2",
                "media_ids[]" to "3",
                "media_attributes[][id]" to "1",
                "media_attributes[][description]" to "A cat",
                "media_attributes[][id]" to "3",
                "media_attributes[][description]" to "A dog"
            ),
            form.fields()
        )
    }

    @Test
    fun `a poll is sent, media fields are left out without media`() {
        val form = StatusEditForm.build(
            NewStatus(
                content = "Vote",
                spoilerText = "CW",
                pollConfig = PollConfig(options = listOf("A", "B"), expiresIn = 3600, multiple = true)
            ),
            descriptions = emptyMap()
        )

        val fields = form.fields()
        assertEquals(
            listOf(
                "status" to "Vote",
                "spoiler_text" to "CW",
                "sensitive" to "false",
                "poll[options][]" to "A",
                "poll[options][]" to "B",
                "poll[expires_in]" to "3600",
                "poll[multiple]" to "true",
                "poll[hide_totals]" to "false"
            ),
            fields
        )
        assertFalse(fields.any { it.first.startsWith("media") })
    }

    private fun FormBody.fields() = (0 until size).map { name(it) to value(it) }
}
