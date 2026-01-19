package com.pfitzer.tootsie.presentation.util

import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import com.pfitzer.tootsie.domain.model.Mention
import com.pfitzer.tootsie.domain.model.Tag

/**
 * Converts HTML content to plain text.
 * Mastodon posts contain HTML formatting that needs to be rendered.
 *
 * @param html HTML string
 * @return Plain text with HTML stripped
 */
fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

/**
 * Type of clickable span in parsed HTML content.
 */
enum class SpanType {
    LINK,      // External URL
    MENTION,   // @username mention
    HASHTAG    // #tag hashtag
}

/**
 * Clickable span information extracted from HTML.
 */
data class ClickableSpan(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val value: String  // URL for links, accountId for mentions, tag name for hashtags
)

/**
 * Parsed HTML content with plain text and clickable spans.
 */
data class ParsedHtmlContent(
    val plainText: String,
    val clickableSpans: List<ClickableSpan>
)

/**
 * Parse HTML content and extract clickable elements (links, mentions, hashtags).
 *
 * @param html HTML content from Mastodon API
 * @param mentions List of mentions in the status
 * @param tags List of hashtags in the status
 * @return Parsed content with plain text and clickable spans
 */
fun parseHtmlContent(
    html: String,
    mentions: List<Mention>,
    tags: List<Tag>
): ParsedHtmlContent {
    if (html.isBlank()) {
        return ParsedHtmlContent("", emptyList())
    }

    // Convert HTML to Spanned to get URLSpan annotations
    val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    val plainText = spanned.toString().trim()

    // Extract all URLSpan elements
    val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java)

    val clickableSpans = mutableListOf<ClickableSpan>()

    for (urlSpan in urlSpans) {
        val spanStart = spanned.getSpanStart(urlSpan)
        val spanEnd = spanned.getSpanEnd(urlSpan)
        val url = urlSpan.url

        // Skip invalid spans
        if (spanStart < 0 || spanEnd > plainText.length || spanStart >= spanEnd) {
            continue
        }

        // Get the visible text of this span
        val spanText = plainText.substring(spanStart, spanEnd)

        // Determine span type by matching against mentions and tags
        val spanType: SpanType
        val spanValue: String

        // Check if this is a mention (by URL or by text starting with @)
        val mention = mentions.firstOrNull { it.url == url }
        if (mention != null) {
            spanType = SpanType.MENTION
            spanValue = mention.id  // Use account ID for navigation
        }
        // Check if text looks like a mention (starts with @)
        else if (spanText.startsWith("@")) {
            // Find mention by username
            val username = spanText.substring(1)  // Remove @ prefix
            val mentionByUsername = mentions.firstOrNull {
                it.username == username || it.acct == username
            }
            if (mentionByUsername != null) {
                spanType = SpanType.MENTION
                spanValue = mentionByUsername.id
            } else {
                // Mention not in list, treat as external link
                spanType = SpanType.LINK
                spanValue = url
            }
        }
        // Check if this is a hashtag (by URL or by text starting with #)
        else if (spanText.startsWith("#")) {
            // Extract tag name (remove # prefix)
            val tagName = spanText.substring(1)
            spanType = SpanType.HASHTAG
            spanValue = tagName
        }
        else {
            val tag = tags.firstOrNull { it.url == url }
            if (tag != null) {
                spanType = SpanType.HASHTAG
                spanValue = tag.name  // Tag name without # prefix
            }
            // Otherwise it's an external link
            else {
                spanType = SpanType.LINK
                spanValue = url
            }
        }

        clickableSpans.add(
            ClickableSpan(
                start = spanStart,
                end = spanEnd,
                type = spanType,
                value = spanValue
            )
        )
    }

    return ParsedHtmlContent(
        plainText = plainText,
        clickableSpans = clickableSpans
    )
}
