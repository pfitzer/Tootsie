package com.pfitzer.tootsie.domain.util

import com.pfitzer.tootsie.domain.util.AutocompleteToken.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutocompleteTokenTest {

    private fun find(textWithCursor: String): AutocompleteToken? {
        val cursor = textWithCursor.indexOf('|')
        return AutocompleteToken.find(textWithCursor.replace("|", ""), cursor)
    }

    @Test
    fun `mention at the cursor`() {
        assertEquals(AutocompleteToken(Type.MENTION, 6, 11, "alic"), find("Hello @alic|"))
        assertEquals("alice@exam", find("@alice@exam|")?.query)
    }

    @Test
    fun `hashtag and emoji`() {
        assertEquals(AutocompleteToken(Type.HASHTAG, 0, 4, "and"), find("#and|"))
        assertEquals(AutocompleteToken(Type.EMOJI, 3, 7, "bla"), find("Hi :bla| there"))
    }

    @Test
    fun `too short or in the middle of a word`() {
        assertNull(find("@a|"))
        assertNull(find("@|"))
        assertNull(find("@ali|ce"))
    }

    @Test
    fun `no token inside other words`() {
        assertNull(find("mail@exam|"))
        assertNull(find("https://example.social/#anch|"))
        assertNull(find("10:30|"))
        assertNull(find("Hello alice|"))
    }

    @Test
    fun `token after a line break or bracket`() {
        assertEquals(Type.MENTION, find("Hi\n@bob|")?.type)
        assertEquals(Type.HASHTAG, find("(#kotl|")?.type)
    }
}
