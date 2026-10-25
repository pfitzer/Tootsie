package com.pfitzer.tootsie.presentation.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U+2028/U+2029 in post and bio HTML (e.g. the bio of @tazgetroete@mastodon.social)
 * must not reach the screen, where Android draws them as empty boxes.
 */
@RunWith(AndroidJUnit4::class)
class HtmlLineSeparatorTest {

    private val bio = "<p>Macht auch mit!   <br /> 💌 Feedback an " +
        "<a href=\"https://taz.de/impressum\">taz.de/impressum</a></p>"

    @Test
    fun parsedContent_hasNoSeparatorsAndLinksStayInPlace() {
        val parsed = parseHtmlContent(bio, mentions = emptyList(), tags = emptyList())

        assertFalse(parsed.plainText.contains(' '))
        assertFalse(parsed.plainText.contains(' '))
        val link = parsed.clickableSpans.single()
        assertEquals("taz.de/impressum", parsed.plainText.substring(link.start, link.end))
    }

    @Test
    fun plainText_hasNoSeparators() {
        val text = htmlToPlainText(bio)

        assertFalse(text.contains(' '))
        assertFalse(text.contains(' '))
    }
}
