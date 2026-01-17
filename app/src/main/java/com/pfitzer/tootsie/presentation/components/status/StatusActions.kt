package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status

/**
 * Displays interaction buttons for a status.
 *
 * @param status Status to show actions for
 * @param onFavoriteClick Callback when favorite button is clicked
 * @param onBoostClick Callback when boost button is clicked
 * @param onReplyClick Callback when reply button is clicked
 * @param onBookmarkClick Callback when bookmark button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun StatusActions(
    status: Status,
    onFavoriteClick: () -> Unit,
    onBoostClick: () -> Unit,
    onReplyClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reply
        ActionButton(
            icon = Icons.AutoMirrored.Filled.Reply,
            count = status.repliesCount,
            isActive = false,
            onClick = onReplyClick,
            contentDescription = stringResource(R.string.cd_reply)
        )

        // Boost
        ActionButton(
            icon = Icons.Filled.Repeat,
            count = status.reblogsCount,
            isActive = status.reblogged,
            onClick = onBoostClick,
            contentDescription = if (status.reblogged) stringResource(R.string.cd_unboost) else stringResource(R.string.cd_boost),
            activeColor = MaterialTheme.colorScheme.tertiary
        )

        // Favorite (using heart icon)
        ActionButton(
            icon = if (status.favourited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            count = status.favouritesCount,
            isActive = status.favourited,
            onClick = onFavoriteClick,
            contentDescription = if (status.favourited) stringResource(R.string.cd_unfavorite) else stringResource(R.string.cd_favorite),
            activeColor = MaterialTheme.colorScheme.primary
        )

        // Bookmark
        ActionButton(
            icon = if (status.bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            count = null,
            isActive = status.bookmarked,
            onClick = onBookmarkClick,
            contentDescription = if (status.bookmarked) stringResource(R.string.cd_remove_bookmark) else stringResource(R.string.cd_bookmark),
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
