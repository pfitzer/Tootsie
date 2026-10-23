package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.pfitzer.tootsie.domain.model.Mention
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
    modifier: Modifier = Modifier
) {
    // Parse HTML to extract plain text and clickable spans
    val parsedContent = parseHtmlContent(content, mentions, tags)

    if (parsedContent.plainText.isBlank()) {
        return
    }

    // Get link color from theme
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodyLarge

    // Build AnnotatedString with clickable spans
    val annotatedString = buildAnnotatedString {
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
                                SpanType.MENTION -> onMentionClick(span.value)
                                SpanType.HASHTAG -> onHashtagClick(span.value)
                                SpanType.LINK -> onLinkClick(span.value)
                            }
                        }
                    ),
                    start = span.start,
                    end = span.end
                )
            }
        }
    }

    // Render text; clicks on links are consumed by their LinkAnnotation,
    // clicks elsewhere fall through to the text click handler
    Text(
        text = annotatedString,
        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = if (onTextClick != null) {
            modifier.clickable(interactionSource = null, indication = null, onClick = onTextClick)
        } else {
            modifier
        }
    )
}
