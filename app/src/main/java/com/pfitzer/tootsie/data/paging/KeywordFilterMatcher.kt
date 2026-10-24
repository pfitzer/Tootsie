package com.pfitzer.tootsie.data.paging

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.Status

/**
 * Applies client-side keyword filters to statuses.
 *
 * Keywords are matched against the visible text of a post (HTML markup removed, entities
 * decoded), its content warning, poll options and media descriptions, like Mastodon does
 * server-side. Matching against raw HTML would hit markup such as `class` or `href`.
 */
class KeywordFilterMatcher(filters: List<Filter>) {

    private class CompiledFilter(val filter: Filter, val patterns: List<Regex>)

    // Compiled once per filter list instead of per status and keyword
    private val compiledFilters = filters.map { filter ->
        CompiledFilter(
            filter = filter,
            patterns = filter.keywords
                .filter { it.keyword.isNotBlank() }
                .map { keyword ->
                    val escaped = Regex.escape(keyword.keyword)
                    Regex(if (keyword.wholeWord) "\\b$escaped\\b" else escaped, RegexOption.IGNORE_CASE)
                }
        )
    }

    val isEmpty: Boolean get() = compiledFilters.all { it.patterns.isEmpty() }

    /**
     * Returns null if the status should be hidden, the status marked with the filter title
     * if it should show a warning, or the status unchanged.
     */
    fun apply(status: Status): Status? {
        if (isEmpty) return status
        val matched = findMatchingFilter(status) ?: return status
        return when (matched.filterAction) {
            FilterAction.HIDE -> null
            FilterAction.WARN -> status.copy(filteredByKeyword = matched.title)
        }
    }

    private fun findMatchingFilter(status: Status): Filter? {
        val text = searchableText(status)
        return compiledFilters
            .firstOrNull { compiled -> compiled.patterns.any { it.containsMatchIn(text) } }
            ?.filter
    }

    private fun searchableText(status: Status): String = buildString {
        appendStatus(status)
        status.reblog?.let { appendStatus(it) }
    }

    private fun StringBuilder.appendStatus(status: Status) {
        append(htmlToText(status.content)).append('\n')
        append(status.spoilerText).append('\n')
        status.poll?.options?.forEach { append(it.title).append('\n') }
        status.mediaAttachments.forEach { media ->
            media.description?.let { append(it).append('\n') }
        }
    }

    companion object {
        private val BLOCK_TAG = Regex("<\\s*(br|/p|/li|/blockquote)[^>]*>", RegexOption.IGNORE_CASE)
        private val TAG = Regex("<[^>]*>")
        private val NUMERIC_ENTITY = Regex("&#(x?)([0-9a-fA-F]+);")
        private val NAMED_ENTITIES = mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&apos;" to "'",
            "&nbsp;" to " "
        )

        /**
         * Plain text of Mastodon status HTML for matching purposes.
         */
        internal fun htmlToText(html: String): String {
            if (html.isEmpty()) return ""
            var text = BLOCK_TAG.replace(html, "\n")
            text = TAG.replace(text, "")
            text = NUMERIC_ENTITY.replace(text) { match ->
                val radix = if (match.groupValues[1].isEmpty()) 10 else 16
                match.groupValues[2].toIntOrNull(radix)
                    ?.takeIf { Character.isValidCodePoint(it) }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
            // &amp; last so "&amp;lt;" becomes "&lt;" and not "<"
            NAMED_ENTITIES.forEach { (entity, replacement) ->
                if (entity != "&amp;") text = text.replace(entity, replacement)
            }
            return text.replace("&amp;", "&")
        }
    }
}
