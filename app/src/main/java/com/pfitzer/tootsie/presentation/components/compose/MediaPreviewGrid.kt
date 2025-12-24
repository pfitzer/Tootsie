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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.MediaAttachment

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
        // Media preview image
        AsyncImage(
            model = media.previewUrl,
            contentDescription = media.description ?: "Media attachment",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
                    contentDescription = "Edit alt text",
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
                    contentDescription = "Remove media",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Alt text indicator (bottom-left)
        if (media.description != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "ALT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
