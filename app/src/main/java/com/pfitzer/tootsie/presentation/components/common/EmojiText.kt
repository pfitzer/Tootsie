package com.pfitzer.tootsie.presentation.components.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.CustomEmoji

private val SHORTCODE = Regex(":([a-zA-Z0-9_]+):")

/**
 * Plain text (e.g. a display name) with the instance's custom emojis (`:shortcode:`)
 * shown as images. Unknown shortcodes stay as text.
 */
@Composable
fun EmojiText(
    text: String,
    emojis: List<CustomEmoji>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (emojis.isEmpty()) {
        Text(text = text, modifier = modifier, style = style, color = color, fontWeight = fontWeight, maxLines = maxLines, overflow = overflow)
        return
    }

    val (annotated, inlineContent) = remember(text, emojis) { buildEmojiText(text, emojis) }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow
    )
}

private fun buildEmojiText(
    text: String,
    emojis: List<CustomEmoji>
): Pair<AnnotatedString, Map<String, InlineTextContent>> = AnnotatedString(text).withCustomEmojis(emojis)

/**
 * Replaces the known custom emoji shortcodes (`:shortcode:`) in this text with inline
 * images. Styles and links of the remaining text are kept; the shortcode stays as
 * alternate text (copying, accessibility). Pass the returned map as `inlineContent`.
 *
 * The static image is used: animated emojis would need a GIF decoder and can be large.
 */
fun AnnotatedString.withCustomEmojis(
    emojis: List<CustomEmoji>
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    if (emojis.isEmpty() || !text.contains(':')) return this to emptyMap()

    val byShortcode = emojis.associateBy { it.shortcode }
    val used = mutableMapOf<String, InlineTextContent>()
    val source = this
    val annotated = buildAnnotatedString {
        var last = 0
        SHORTCODE.findAll(source.text).forEach { match ->
            val emoji = byShortcode[match.groupValues[1]] ?: return@forEach
            append(source.subSequence(last, match.range.first))
            appendInlineContent(id = emoji.shortcode, alternateText = match.value)
            used.getOrPut(emoji.shortcode) {
                InlineTextContent(
                    Placeholder(width = 1.2.em, height = 1.2.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
                ) {
                    AsyncImage(
                        model = emoji.staticUrl.ifBlank { emoji.url },
                        contentDescription = match.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            last = match.range.last + 1
        }
        append(source.subSequence(last, source.length))
    }
    return annotated to used
}
