package com.pfitzer.tootsie.presentation.components.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R

/**
 * Dialog for reporting a post or account to instance moderators.
 *
 * @param showForwardOption Whether to show the "forward to remote instance" checkbox
 * @param onConfirm Callback with (category, comment, forward)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ReportDialog(
    showForwardOption: Boolean = false,
    onConfirm: (category: String, comment: String?, forward: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "spam" to stringResource(R.string.report_category_spam),
        "violation" to stringResource(R.string.report_category_violation),
        "other" to stringResource(R.string.report_category_other)
    )
    var selectedCategory by remember { mutableStateOf("other") }
    var comment by remember { mutableStateOf("") }
    var forward by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category selection
                Column(modifier = Modifier.selectableGroup()) {
                    categories.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedCategory == value,
                                    onClick = { selectedCategory = value },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == value,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Comment field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.report_comment_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Forward to remote instance checkbox
                if (showForwardOption) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = forward,
                            onCheckedChange = { forward = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.report_forward),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedCategory,
                        comment.trim().ifBlank { null },
                        forward
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
