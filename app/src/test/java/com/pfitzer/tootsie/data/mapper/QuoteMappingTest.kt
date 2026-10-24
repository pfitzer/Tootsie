package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.StatusDto
import com.pfitzer.tootsie.domain.model.QuotePermission
import com.pfitzer.tootsie.domain.model.QuoteState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteMappingTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val account = """{"id":"1","username":"alice","acct":"alice@example.social",
        "display_name":"Alice","note":"","url":"https://example.social/@alice","avatar":"",
        "header":"","locked":false,"bot":false,"followers_count":0,"following_count":0,
        "statuses_count":0,"created_at":"2025-01-01T00:00:00.000Z"}"""

    private fun status(id: String, content: String, extra: String = "") = """{"id":"$id",
        "uri":"https://example.social/statuses/$id","account":$account,"content":"$content",
        "created_at":"2026-10-24T06:00:00.000Z","visibility":"public","sensitive":false,
        "spoiler_text":"","reblogs_count":0,"favourites_count":0,"replies_count":0$extra}"""

    private fun parse(value: String) = json.decodeFromString<StatusDto>(value).toDomain()

    @Test
    fun `accepted quote with quoted status and policy`() {
        val quoted = status("2", "<p>Original</p>")
        val result = parse(
            status(
                "1",
                """<p class=\"quote-inline\">RE: <a href=\"https://example.social/@alice/2\">example.social/@alice/2</a></p><p>My comment</p>""",
                ""","quote":{"state":"accepted","quoted_status":$quoted},
                "quote_approval":{"automatic":["public"],"manual":[],"current_user":"automatic"}"""
            )
        )

        assertEquals(QuoteState.ACCEPTED, result.quote?.state)
        assertEquals("2", result.quote?.quotedStatus?.id)
        assertEquals("2", result.quote?.quotedStatusId)
        assertEquals(QuotePermission.AUTOMATIC, result.quotePermission)
        // The RE: fallback paragraph is removed, the comment stays
        assertEquals("<p>My comment</p>", result.content)
    }

    @Test
    fun `shallow quote and unknown state`() {
        val result = parse(status("1", "<p>x</p>", ""","quote":{"state":"something_new","quoted_status_id":"9"}"""))

        assertEquals(QuoteState.UNAUTHORIZED, result.quote?.state)
        assertNull(result.quote?.quotedStatus)
        assertEquals("9", result.quote?.quotedStatusId)
    }

    @Test
    fun `quote permission values`() {
        fun permission(currentUser: String) = parse(
            status("1", "<p>x</p>", ""","quote_approval":{"automatic":[],"manual":[],"current_user":"$currentUser"}""")
        ).quotePermission

        assertEquals(QuotePermission.AUTOMATIC, permission("automatic"))
        assertEquals(QuotePermission.MANUAL, permission("manual"))
        assertEquals(QuotePermission.DENIED, permission("denied"))
        // "unknown" is treated as denied, as recommended by the API documentation
        assertEquals(QuotePermission.DENIED, permission("unknown"))
        assertTrue(QuotePermission.MANUAL.canQuote)
        assertFalse(QuotePermission.DENIED.canQuote)
    }

    @Test
    fun `server without quote support`() {
        val result = parse(status("1", """<p class=\"quote-inline\">kept</p>"""))

        assertNull(result.quote)
        assertNull(result.quotePermission)
        // Without a quote the paragraph is left untouched
        assertEquals("""<p class="quote-inline">kept</p>""", result.content)
    }
}
