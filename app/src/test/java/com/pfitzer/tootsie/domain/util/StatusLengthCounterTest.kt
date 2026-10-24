package com.pfitzer.tootsie.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusLengthCounterTest {

    @Test
    fun `plain text counts characters`() {
        assertEquals(5, StatusLengthCounter.count("Hallo"))
        assertEquals(0, StatusLengthCounter.count(""))
    }

    @Test
    fun `every url counts as 23 characters`() {
        val longUrl = "https://example.social/@alice/112233445566778899?with=a&very=long&query=string"
        assertEquals(23, StatusLengthCounter.count(longUrl))
        assertEquals(23, StatusLengthCounter.count("http://a.de"))
        // "Look: " + URL + "."; the trailing dot isn't part of the URL
        assertEquals(6 + 23 + 1, StatusLengthCounter.count("Look: $longUrl."))
    }

    @Test
    fun `url length follows the instance setting`() {
        assertEquals(30, StatusLengthCounter.count("https://example.social", charactersReservedPerUrl = 30))
    }

    @Test
    fun `remote mentions count only the local part`() {
        assertEquals("@alice".length, StatusLengthCounter.count("@alice@example.social"))
        assertEquals("Hi @alice and @bob!".length, StatusLengthCounter.count("Hi @alice@example.social and @bob!"))
    }

    @Test
    fun `email addresses are not mentions`() {
        assertEquals("mail@example.social".length, StatusLengthCounter.count("mail@example.social"))
    }

    @Test
    fun `content warning counts as well`() {
        assertEquals(5 + 8, StatusLengthCounter.count("Hallo", spoilerText = "Spoilers"))
    }

    @Test
    fun `emoji count as one character`() {
        assertEquals(1, StatusLengthCounter.count("👍"))
        assertEquals(1, StatusLengthCounter.count("👨‍👩‍👧"))
        assertEquals(2, StatusLengthCounter.count("ä😀"))
    }
}
