package com.pfitzer.tootsie.presentation.components.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import com.pfitzer.tootsie.domain.model.CustomEmoji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomEmojiTextTest {

    private val blobcat = CustomEmoji(shortcode = "blobcat", url = "https://x/a.gif", staticUrl = "https://x/a.png")

    @Test
    fun `known shortcodes become inline content, unknown ones stay text`() {
        val (text, inline) = AnnotatedString("Hi :blobcat: and :unknown: :blobcat:").withCustomEmojis(listOf(blobcat))

        // The shortcode stays as alternate text
        assertEquals("Hi :blobcat: and :unknown: :blobcat:", text.text)
        assertEquals(setOf("blobcat"), inline.keys)
        assertEquals(2, text.getStringAnnotations(0, text.length).count { it.item == "blobcat" })
    }

    @Test
    fun `links after an emoji keep their position`() {
        val source = buildAnnotatedString {
            append("Wow :blobcat: see #cats")
            addLink(LinkAnnotation.Url("https://x/tags/cats"), start = 18, end = 23)
        }

        val (text, _) = source.withCustomEmojis(listOf(blobcat))

        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("#cats", text.text.substring(link.start, link.end))
    }

    @Test
    fun `text without emojis is returned unchanged`() {
        val source = AnnotatedString("No emojis here")
        val (text, inline) = source.withCustomEmojis(listOf(blobcat))

        assertSame(source, text)
        assertTrue(inline.isEmpty())
    }
}
