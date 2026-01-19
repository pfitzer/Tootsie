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
 * Converts HTML content to AnnotatedString for rich text editing.
 * Supports basic tags like <b>, <i>, <u>, <a>.
 */
fun htmlToAnnotatedString(html: String): androidx.compose.ui.text.AnnotatedString {
    if (html.isBlank()) return androidx.compose.ui.text.AnnotatedString("")

    val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    builder.append(spanned.toString())

    val spans = spanned.getSpans(0, spanned.length, Any::class.java)
    for (span in spans) {
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        when (span) {
            is android.text.style.StyleSpan -> {
                when (span.style) {
                    android.graphics.Typeface.BOLD -> builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        start,
                        end
                    )
                    android.graphics.Typeface.ITALIC -> builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        start,
                        end
                    )
                    android.graphics.Typeface.BOLD_ITALIC -> builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }
            }
            is android.text.style.UnderlineSpan -> builder.addStyle(
                androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                start,
                end
            )
            is android.text.style.URLSpan -> {
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    start,
                    end
                )
                builder.addStringAnnotation("LINK", span.url, start, end)
            }
        }
    }
    return builder.toAnnotatedString()
}

/**
 * Converts AnnotatedString back to basic HTML for Mastodon API.
 */
fun annotatedStringToHtml(annotatedString: androidx.compose.ui.text.AnnotatedString): String {
    val text = annotatedString.text
    val sb = StringBuilder()
    
    // This is a simplified conversion. A more robust one would handle nested spans correctly.
    // For bio editing, this should be sufficient.
    var lastIndex = 0
    
    // We sort spans by start position to process them in order
    val allSpans = (annotatedString.spanStyles.map { it to "style" } + 
                   annotatedString.getStringAnnotations(0, text.length).map { it to "annotation" })
        .sortedBy { (span, _) -> 
            when (span) {
                is androidx.compose.ui.text.AnnotatedString.Range<*> -> span.start
                else -> 0
            }
        }

    // Since we can't easily convert back to HTML with nested tags this way, 
    // we'll use a simpler approach: process character by character or use a more advanced logic.
    // However, Mastodon Bio is usually simple.
    
    // Let's use a simpler character-based approach for reliability
    for (i in text.indices) {
        val char = text[i]
        
        // Check for tags starting at this index
        annotatedString.spanStyles.filter { it.start == i }.forEach { span ->
            if (span.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) sb.append("<b>")
            if (span.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic) sb.append("<i>")
            if (span.item.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline) sb.append("<u>")
        }
        annotatedString.getStringAnnotations("LINK", i, i + 1).filter { it.start == i }.forEach { span ->
            sb.append("<a href=\"").append(span.item).append("\">")
        }

        // Append character (with basic HTML escaping)
        when (char) {
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '&' -> sb.append("&amp;")
            '\n' -> sb.append("<br>")
            else -> sb.append(char)
        }

        // Check for tags ending at this index + 1
        annotatedString.getStringAnnotations("LINK", i, i + 1).filter { it.end == i + 1 }.forEach { _ ->
            sb.append("</a>")
        }
        annotatedString.spanStyles.filter { it.end == i + 1 }.forEach { span ->
            if (span.item.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline) sb.append("</u>")
            if (span.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic) sb.append("</i>")
            if (span.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) sb.append("</b>")
        }
    }
    
    return sb.toString()
}

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
