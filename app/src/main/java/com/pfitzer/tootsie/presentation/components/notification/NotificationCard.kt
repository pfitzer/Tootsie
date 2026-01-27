package com.pfitzer.tootsie.presentation.components.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.model.NotificationType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.status.RichTextContent
import com.pfitzer.tootsie.presentation.components.status.StatusActions

/**
 * Card component for displaying a notification.
 * Different notification types are displayed with appropriate icons and messages.
 */
@Composable
fun NotificationCard(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onAvatarClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onReplyClick: (Status) -> Unit = {},
    onBoostClick: (Status) -> Unit = {},
    onFavoriteClick: (Status) -> Unit = {},
    onBookmarkClick: (Status) -> Unit = {},
    onShareClick: (Status) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNotificationClick(notification) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification type icon
            NotificationTypeIcon(
                type = notification.type,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Top)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Account info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = notification.account.avatar,
                        contentDescription = stringResource(R.string.cd_avatar),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onAvatarClick(notification.account.id) },
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.account.displayName.ifBlank { notification.account.username },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = GetNotificationMessage(notification.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status content (if applicable)
                if (notification.status != null) {
                    RichTextContent(
                        content = notification.status.content,
                        mentions = notification.status.mentions,
                        tags = notification.status.tags,
                        onMentionClick = onMentionClick,
                        onHashtagClick = onHashtagClick,
                        onLinkClick = onLinkClick,
                        onTextClick = null, // Don't handle text clicks separately
                        modifier = Modifier.padding(start = 48.dp)
                    )

                    // Action buttons only for mentions (where user can interact with the status)
                    if (notification.type == NotificationType.MENTION) {
                        StatusActions(
                            status = notification.status,
                            onReplyClick = { onReplyClick(notification.status) },
                            onBoostClick = { onBoostClick(notification.status) },
                            onFavoriteClick = { onFavoriteClick(notification.status) },
                            onBookmarkClick = { onBookmarkClick(notification.status) },
                            onShareClick = { onShareClick(notification.status) },
                            modifier = Modifier.padding(start = 48.dp, top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icon for notification type.
 */
@Composable
private fun NotificationTypeIcon(
    type: NotificationType,
    modifier: Modifier = Modifier
) {
    val iconData = when (type) {
        NotificationType.FOLLOW -> Pair(Icons.Default.Person, MaterialTheme.colorScheme.primary)
        NotificationType.FOLLOW_REQUEST -> Pair(Icons.Default.Person, MaterialTheme.colorScheme.primary)
        NotificationType.MENTION -> Pair(Icons.Default.Email, MaterialTheme.colorScheme.tertiary)
        NotificationType.REBLOG -> Pair(Icons.Default.Refresh, MaterialTheme.colorScheme.secondary)
        NotificationType.FAVOURITE -> Pair(Icons.Default.Favorite, MaterialTheme.colorScheme.error)
        NotificationType.POLL -> Pair(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
        NotificationType.STATUS -> Pair(Icons.Default.Edit, MaterialTheme.colorScheme.primary)
        NotificationType.UPDATE -> Pair(Icons.Default.Edit, MaterialTheme.colorScheme.tertiary)
        NotificationType.UNKNOWN -> Pair(Icons.Default.Notifications, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Icon(
        imageVector = iconData.first,
        contentDescription = null,
        modifier = modifier,
        tint = iconData.second
    )
}

/**
 * Get human-readable message for notification type.
 */
@Composable
private fun GetNotificationMessage(type: NotificationType): String {
    return when (type) {
        NotificationType.FOLLOW -> stringResource(R.string.notification_followed_you)
        NotificationType.FOLLOW_REQUEST -> stringResource(R.string.notification_requested_follow)
        NotificationType.MENTION -> stringResource(R.string.notification_mentioned_you)
        NotificationType.REBLOG -> stringResource(R.string.notification_boosted_post)
        NotificationType.FAVOURITE -> stringResource(R.string.notification_favourited_post)
        NotificationType.POLL -> stringResource(R.string.notification_poll_ended)
        NotificationType.STATUS -> stringResource(R.string.notification_new_status)
        NotificationType.UPDATE -> stringResource(R.string.notification_edited_post)
        NotificationType.UNKNOWN -> stringResource(R.string.notification_generic)
    }
}
