package com.pfitzer.tootsie.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LineSeparatorTest {

    @Test
    fun `line and paragraph separators become spaces, keeping the length`() {
        val text = "Macht auch mit!   Ende "

        val cleaned = text.withoutLineSeparators()

        assertEquals("Macht auch mit!   Ende ", cleaned)
        assertEquals(text.length, cleaned.length)
    }

    @Test
    fun `text without separators is returned as is`() {
        val text = "Hallo 🏳️‍🌈 🇨🇭"

        assertSame(text, text.withoutLineSeparators())
    }
}
