package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.model.Mention
import com.pfitzer.tootsie.presentation.components.common.withCustomEmojis
import com.pfitzer.tootsie.domain.model.Tag
import com.pfitzer.tootsie.presentation.util.SpanType
import com.pfitzer.tootsie.presentation.util.parseHtmlContent

/**
 * Rich text content renderer for Mastodon posts.
 * Parses HTML and renders clickable mentions, hashtags, and links.
 *
 * @param content HTML content from Mastodon API
 * @param mentions List of mentions in the post
 * @param tags List of hashtags in the post
 * @param onMentionClick Callback when a mention is clicked (receives accountId)
 * @param onHashtagClick Callback when a hashtag is clicked (receives tag name)
 * @param onLinkClick Callback when an external link is clicked (receives URL)
 * @param onTextClick Optional callback when non-linked text is clicked (e.g., to view status detail)
 * @param modifier Modifier for the text component
 * @param emojis Custom emojis of the post or account, shown as images instead of `:shortcode:`
 */
@Composable
fun RichTextContent(
    content: String,
    mentions: List<Mention>,
    tags: List<Tag>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onTextClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emojis: List<CustomEmoji> = emptyList()
) {
    // Parsing the HTML is the most expensive part of rendering a post, so it only runs
    // when the content changes, not on every recomposition (e.g. while scrolling)
    val parsedContent = remember(content, mentions, tags) { parseHtmlContent(content, mentions, tags) }

    if (parsedContent.plainText.isBlank()) {
        return
    }

    // Get link color from theme
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodyLarge

    // The link annotations call the latest callbacks, so new lambdas from the caller
    // don't force the text to be rebuilt
    val currentOnMentionClick by rememberUpdatedState(onMentionClick)
    val currentOnHashtagClick by rememberUpdatedState(onHashtagClick)
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)

    // Build AnnotatedString with clickable spans and custom emojis
    val (text, inlineContent) = remember(parsedContent, linkColor, emojis) {
        buildAnnotatedString {
            // Append the full plain text
            append(parsedContent.plainText)

            // Add styling and annotations for each clickable span
            parsedContent.clickableSpans.forEach { span ->
                // Ensure indices are within bounds
                if (span.start >= 0 && span.end <= parsedContent.plainText.length && span.start < span.end) {
                    // Link annotation handles both styling and click handling
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = span.type.name,  // "LINK", "MENTION", or "HASHTAG"
                            styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                            linkInteractionListener = {
                                when (span.type) {
                                    SpanType.MENTION -> currentOnMentionClick(span.value)
                                    SpanType.HASHTAG -> currentOnHashtagClick(span.value)
                                    SpanType.LINK -> currentOnLinkClick(span.value)
                                }
                            }
                        ),
                        start = span.start,
                        end = span.end
                    )
                }
            }
        }.withCustomEmojis(emojis)
    }

    // Render text; clicks on links are consumed by their LinkAnnotation,
    // clicks elsewhere fall through to the text click handler
    Text(
        text = text,
        inlineContent = inlineContent,
        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = if (onTextClick != null) {
            modifier.clickable(interactionSource = null, indication = null, onClick = onTextClick)
        } else {
            modifier
        }
    )
}
