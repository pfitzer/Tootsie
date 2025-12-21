package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.domain.model.Status

/**
 * Displays interaction buttons for a status.
 *
 * @param status Status to show actions for
 * @param onFavoriteClick Callback when favorite button is clicked
 * @param onBoostClick Callback when boost button is clicked
 * @param onReplyClick Callback when reply button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun StatusActions(
    status: Status,
    onFavoriteClick: () -> Unit,
    onBoostClick: () -> Unit,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reply
        ActionButton(
            icon = Icons.Filled.ArrowForward,
            count = status.repliesCount,
            isActive = false,
            onClick = onReplyClick,
            contentDescription = "Reply"
        )

        // Boost
        ActionButton(
            icon = Icons.Filled.Share,
            count = status.reblogsCount,
            isActive = status.reblogged,
            onClick = onBoostClick,
            contentDescription = if (status.reblogged) "Unboost" else "Boost",
            activeColor = MaterialTheme.colorScheme.tertiary
        )

        // Favorite
        ActionButton(
            icon = if (status.favourited) Icons.Filled.Star else Icons.Outlined.Star,
            count = status.favouritesCount,
            isActive = status.favourited,
            onClick = onFavoriteClick,
            contentDescription = if (status.favourited) "Unfavorite" else "Favorite",
            activeColor = MaterialTheme.colorScheme.primary
        )

        // Bookmark (placeholder)
        ActionButton(
            icon = if (status.bookmarked) Icons.Filled.Star else Icons.Outlined.Star,
            count = null,
            isActive = status.bookmarked,
            onClick = { /* TODO: Implement bookmark */ },
            contentDescription = if (status.bookmarked) "Remove bookmark" else "Bookmark",
            activeColor = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Single action button with icon and optional count.
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    count: Int?,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    activeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) {
        activeColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        if (count != null && count > 0) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

/**
 * Formats interaction counts for display.
 * Examples: 1234 -> "1.2K", 1500000 -> "1.5M"
 */
private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1000000.0)
    }
}
