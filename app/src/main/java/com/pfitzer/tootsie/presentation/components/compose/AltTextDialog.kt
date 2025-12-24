package com.pfitzer.tootsie.presentation.components.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.domain.model.MediaAttachment

/**
 * Dialog for editing alt text (accessibility description) for media attachments.
 *
 * @param media The media attachment to edit alt text for
 * @param onDismiss Callback when dialog is dismissed
 * @param onSave Callback when alt text is saved (attachmentId, description)
 */
@Composable
fun AltTextDialog(
    media: MediaAttachment,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var altText by remember { mutableStateOf(media.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Alt Text")
        },
        text = {
            Column {
                Text(
                    text = "Describe this image for people who are blind or have low vision.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text("Description") },
                    placeholder = { Text("A person walking in a park...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    supportingText = {
                        Text("${altText.length} / 1500 characters")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(media.id, altText.trim())
                    onDismiss()
                },
                enabled = altText.length <= 1500
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
