package com.pfitzer.tootsie.presentation.components.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pfitzer.tootsie.domain.model.CustomEmoji

/**
 * Dialog for picking custom emojis.
 * Displays all available instance custom emojis in a grid.
 *
 * @param emojis List of custom emojis to display
 * @param onEmojiSelected Callback when an emoji is selected (receives shortcode)
 * @param onDismiss Callback to dismiss the dialog
 * @param isLoading Whether emojis are currently loading
 */
@Composable
fun EmojiPickerDialog(
    emojis: List<CustomEmoji>,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Emojis") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    emojis.isEmpty() -> {
                        Text(
                            text = "No custom emojis available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = emojis.filter { it.visibleInPicker },
                                key = { it.shortcode }
                            ) { emoji ->
                                EmojiItem(
                                    emoji = emoji,
                                    onClick = {
                                        onEmojiSelected(emoji.shortcode)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

/**
 * Single emoji item in the grid.
 */
@Composable
private fun EmojiItem(
    emoji: CustomEmoji,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(emoji.url)
                    .crossfade(true)
                    .build(),
                contentDescription = emoji.shortcode,
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
            )
        }
    }
}
