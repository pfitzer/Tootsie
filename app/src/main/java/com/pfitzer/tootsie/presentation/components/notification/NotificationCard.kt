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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.model.NotificationType

/**
 * Card component for displaying a notification.
 * Different notification types are displayed with appropriate icons and messages.
 */
@Composable
fun NotificationCard(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onAvatarClick: (String) -> Unit,
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
                        contentDescription = "Avatar",
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
                            text = getNotificationMessage(notification.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status content (if applicable)
                if (notification.status != null) {
                    Text(
                        text = notification.status.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 48.dp)
                    )
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
private fun getNotificationMessage(type: NotificationType): String {
    return when (type) {
        NotificationType.FOLLOW -> "followed you"
        NotificationType.FOLLOW_REQUEST -> "requested to follow you"
        NotificationType.MENTION -> "mentioned you"
        NotificationType.REBLOG -> "boosted your post"
        NotificationType.FAVOURITE -> "favourited your post"
        NotificationType.POLL -> "poll has ended"
        NotificationType.STATUS -> "posted a new status"
        NotificationType.UPDATE -> "edited a post"
        NotificationType.UNKNOWN -> "notification"
    }
}
