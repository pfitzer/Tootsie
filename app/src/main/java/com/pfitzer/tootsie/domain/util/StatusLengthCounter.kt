package com.pfitzer.tootsie.domain.util

import java.text.BreakIterator

/**
 * Counts the length of a post the way Mastodon's StatusLengthValidator does:
 *
 * - every URL counts as [charactersReservedPerUrl] characters, regardless of its length
 * - a mention of a remote account counts only its local part (`@user@domain` → `@user`)
 * - the content warning counts as well
 * - characters are counted as grapheme clusters, so an emoji counts as one
 */
object StatusLengthCounter {

    // http(s) URLs not glued to a preceding word; trailing punctuation isn't part of the URL
    private val URL = Regex("""(?<![\w/])https?://[^\s<>"]*[^\s<>".,;:!?)\]'}]""", RegexOption.IGNORE_CASE)

    // Same as Mastodon's Account::MENTION_RE; group 1 is the local part
    private val MENTION = Regex(
        """(?<=^|[^/\w])@([a-z0-9_]+(?:[a-z0-9_.-]+[a-z0-9_]+)?)(?:@[\w.-]+\w+)?""",
        RegexOption.IGNORE_CASE
    )

    fun count(text: String, spoilerText: String = "", charactersReservedPerUrl: Int = 23): Int {
        val urlPlaceholder = "x".repeat(charactersReservedPerUrl)
        val countable = text
            .replace(URL, urlPlaceholder)
            .replace(MENTION) { "@" + it.groupValues[1] }
        return graphemeCount(countable) + graphemeCount(spoilerText)
    }

    private fun graphemeCount(text: String): Int {
        if (text.isEmpty()) return 0
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }
}
