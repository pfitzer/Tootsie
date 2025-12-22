package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.util.formatRelativeTime
import com.pfitzer.tootsie.presentation.util.htmlToPlainText

/**
 * Card component that displays a single Mastodon status (post).
 *
 * @param status Status to display
 * @param onStatusClick Callback when status is clicked
 * @param onAvatarClick Callback when avatar is clicked
 * @param onFavoriteClick Callback when favorite button is clicked
 * @param onBoostClick Callback when boost button is clicked
 * @param onReplyClick Callback when reply button is clicked
 * @param onBookmarkClick Callback when bookmark button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun StatusCard(
    status: Status,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    modifier: Modifier = Modifier
) {
    // If this is a boost/reblog, show the boosted status
    val displayStatus = status.reblog ?: status
    val isReblog = status.reblog != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStatusClick(displayStatus.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Boost indicator
            if (isReblog) {
                BoostIndicator(
                    boosterName = status.account.displayName,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Account header
            StatusHeader(
                account = displayStatus.account,
                timestamp = displayStatus.createdAt,
                onAvatarClick = { onAvatarClick(displayStatus.account.id) },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Content warning
            if (displayStatus.spoilerText.isNotBlank()) {
                ContentWarning(
                    text = displayStatus.spoilerText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Status content
            StatusContent(
                content = displayStatus.content,
                sensitive = displayStatus.sensitive,
                spoilerText = displayStatus.spoilerText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Media attachments
            if (displayStatus.mediaAttachments.isNotEmpty()) {
                MediaAttachmentGrid(
                    attachments = displayStatus.mediaAttachments,
                    sensitive = displayStatus.sensitive,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Interaction counts and buttons
            StatusActions(
                status = displayStatus,
                onFavoriteClick = { onFavoriteClick(displayStatus) },
                onBoostClick = { onBoostClick(displayStatus) },
                onReplyClick = { onReplyClick(displayStatus) },
                onBookmarkClick = { onBookmarkClick(displayStatus) }
            )
        }
    }
}

/**
 * Displays boost indicator when a status is reblogged.
 */
@Composable
private fun BoostIndicator(
    boosterName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$boosterName boosted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Displays account information and timestamp.
 */
@Composable
private fun StatusHeader(
    account: com.pfitzer.tootsie.domain.model.Account,
    timestamp: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = account.avatar,
            contentDescription = "${account.displayName}'s avatar",
            modifier = Modifier
                .size(48.dp)
                .clickable { onAvatarClick() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${account.acct}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatRelativeTime(timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays content warning text.
 */
@Composable
private fun ContentWarning(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "CW: $text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Displays status content (text).
 */
@Composable
private fun StatusContent(
    content: String,
    sensitive: Boolean,
    spoilerText: String,
    modifier: Modifier = Modifier
) {
    // Convert HTML to plain text
    val plainText = htmlToPlainText(content)

    if (plainText.isNotBlank()) {
        Text(
            text = plainText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Displays media attachments in a grid.
 */
@Composable
private fun MediaAttachmentGrid(
    attachments: List<com.pfitzer.tootsie.domain.model.MediaAttachment>,
    sensitive: Boolean,
    modifier: Modifier = Modifier
) {
    // Simplified for now - just show count
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${attachments.size} media attachment${if (attachments.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
