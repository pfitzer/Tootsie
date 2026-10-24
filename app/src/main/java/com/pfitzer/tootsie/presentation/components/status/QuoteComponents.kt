package com.pfitzer.tootsie.presentation.components.status

import com.pfitzer.tootsie.presentation.components.common.EmojiText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Quote
import com.pfitzer.tootsie.domain.model.QuotePermission
import com.pfitzer.tootsie.domain.model.QuoteState
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.util.htmlToPlainText

/**
 * Opens the compose screen quoting the given status. Provided once by the nav host, so the
 * many screens showing statuses don't each need a callback. Null: quoting not available.
 */
val LocalOnQuoteStatus = staticCompositionLocalOf<((Status) -> Unit)?> { null }

/**
 * Choice shown when tapping the boost button on servers with quote support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostOrQuoteSheet(
    status: Status,
    onBoost: () -> Unit,
    onQuote: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val permission = status.quotePermission ?: QuotePermission.DENIED
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(stringResource(if (status.reblogged) R.string.status_unboost else R.string.status_boost))
                },
                leadingContent = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                modifier = Modifier.clickable {
                    onBoost()
                    onDismissRequest()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.status_quote)) },
                supportingContent = when (permission) {
                    QuotePermission.AUTOMATIC -> null
                    QuotePermission.MANUAL -> {
                        { Text(stringResource(R.string.status_quote_needs_approval)) }
                    }
                    QuotePermission.DENIED -> {
                        { Text(stringResource(R.string.status_quote_not_allowed)) }
                    }
                },
                leadingContent = { Icon(Icons.Filled.FormatQuote, contentDescription = null) },
                colors = if (permission.canQuote) {
                    ListItemDefaults.colors()
                } else {
                    ListItemDefaults.colors(
                        headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        leadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                },
                modifier = Modifier.clickable(enabled = permission.canQuote) {
                    onQuote()
                    onDismissRequest()
                }
            )
        }
    }
}

/**
 * Compact card for a quoted status inside a post (or in the compose preview).
 */
@Composable
fun QuotedStatusCard(
    quote: Quote,
    onClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val quoted = quote.quotedStatus?.takeIf { quote.state == QuoteState.ACCEPTED }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (quoted != null && onClick != null) {
                    Modifier.clickable { onClick(quoted.id) }
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        if (quoted == null) {
            Text(
                text = stringResource(
                    if (quote.state == QuoteState.PENDING) R.string.status_quote_pending
                    else R.string.status_quote_unavailable
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            QuotedStatusContent(quoted)
        }
    }
}

@Composable
private fun QuotedStatusContent(status: Status) {
    val plainText = remember(status.content) { htmlToPlainText(status.content) }
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(
                url = status.account.avatar,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            EmojiText(
                text = status.account.displayName.ifBlank { status.account.username },
                emojis = status.account.emojis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "@${status.account.acct}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.padding(top = 6.dp))
        // Respect the quoted post's content warning: show only the warning text
        if (status.spoilerText.isNotBlank()) {
            EmojiText(
                text = stringResource(R.string.status_content_warning, status.spoilerText),
                emojis = status.emojis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (plainText.isNotBlank()) {
            EmojiText(
                text = plainText,
                emojis = status.emojis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (status.mediaAttachments.isNotEmpty()) {
            Text(
                text = pluralStringResource(
                    R.plurals.status_quote_media_count,
                    status.mediaAttachments.size,
                    status.mediaAttachments.size
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
