package com.pfitzer.tootsie.presentation.components.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.domain.model.TimelineFilter

/**
 * Dialog for configuring timeline filters.
 *
 * @param currentFilter Current filter settings
 * @param onDismiss Callback when dialog is dismissed
 * @param onApply Callback when user applies new filter settings
 */
@Composable
fun TimelineFilterDialog(
    currentFilter: TimelineFilter,
    onDismiss: () -> Unit,
    onApply: (TimelineFilter) -> Unit
) {
    var hideBoosts by remember { mutableStateOf(currentFilter.hideBoosts) }
    var hideReplies by remember { mutableStateOf(currentFilter.hideReplies) }
    var showOnlyMedia by remember { mutableStateOf(currentFilter.showOnlyMedia) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Timeline Filters")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hide boosts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hide boosts")
                    Switch(
                        checked = hideBoosts,
                        onCheckedChange = { hideBoosts = it }
                    )
                }

                // Hide replies
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hide replies")
                    Switch(
                        checked = hideReplies,
                        onCheckedChange = { hideReplies = it }
                    )
                }

                // Show only media
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show only posts with media")
                    Switch(
                        checked = showOnlyMedia,
                        onCheckedChange = { showOnlyMedia = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        TimelineFilter(
                            hideBoosts = hideBoosts,
                            hideReplies = hideReplies,
                            showOnlyMedia = showOnlyMedia
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
