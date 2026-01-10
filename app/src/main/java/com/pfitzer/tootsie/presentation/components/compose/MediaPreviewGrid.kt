package com.pfitzer.tootsie.presentation.components.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType

/**
 * Grid displaying media attachment previews with edit and remove options.
 *
 * @param mediaAttachments List of media attachments to display
 * @param onRemoveMedia Callback when remove button is clicked
 * @param onEditAltText Callback when edit alt text button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun MediaPreviewGrid(
    mediaAttachments: List<MediaAttachment>,
    onRemoveMedia: (String) -> Unit,
    onEditAltText: (MediaAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaAttachments.isEmpty()) return

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaAttachments) { media ->
            MediaPreviewItem(
                media = media,
                onRemove = { onRemoveMedia(media.id) },
                onEditAltText = { onEditAltText(media) }
            )
        }
    }
}

/**
 * Single media preview item with controls.
 */
@Composable
private fun MediaPreviewItem(
    media: MediaAttachment,
    onRemove: () -> Unit,
    onEditAltText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Media preview - show different UI based on type
        when (media.type) {
            MediaType.AUDIO -> {
                // Audio file - show "AUDIO" text instead of preview image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.media_label_audio),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Image or video - show preview image
                AsyncImage(
                    model = media.previewUrl,
                    contentDescription = media.description ?: "Media attachment",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Overlay gradient for better button visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )

        // Video/GIF play button overlay (centered)
        if (media.type == MediaType.VIDEO || media.type == MediaType.GIFV) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.cd_video),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Top-right controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Edit alt text button
            IconButton(
                onClick = onEditAltText,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit_alt_text),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove_media),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Bottom-left indicators (Alt text and media type)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Media type indicator for video/audio
            if (media.type == MediaType.VIDEO || media.type == MediaType.AUDIO || media.type == MediaType.GIFV) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (media.type) {
                            MediaType.VIDEO -> "VIDEO"
                            MediaType.AUDIO -> "AUDIO"
                            MediaType.GIFV -> "GIF"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Alt text indicator
            if (media.description != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.media_label_alt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
