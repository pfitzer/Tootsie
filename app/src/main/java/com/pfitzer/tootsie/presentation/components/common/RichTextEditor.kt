package com.pfitzer.tootsie.presentation.components.common

import com.pfitzer.tootsie.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * A basic rich text editor for HTML content.
 * Supports bold, italic, and links.
 */
@Composable
fun RichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var showLinkDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolbarButton(
                    icon = Icons.Default.FormatBold,
                    onClick = {
                        onValueChange(applyStyle(value, SpanStyle(fontWeight = FontWeight.Bold)))
                    },
                    contentDescription = stringResource(R.string.cd_bold)
                )
                ToolbarButton(
                    icon = Icons.Default.FormatItalic,
                    onClick = {
                        onValueChange(applyStyle(value, SpanStyle(fontStyle = FontStyle.Italic)))
                    },
                    contentDescription = stringResource(R.string.cd_italic)
                )
                ToolbarButton(
                    icon = Icons.Default.Link,
                    onClick = { showLinkDialog = true },
                    contentDescription = stringResource(R.string.cd_insert_link)
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            label = label,
            enabled = enabled,
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
        )
    }

    if (showLinkDialog) {
        LinkInputDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url ->
                onValueChange(applyLink(value, url))
                showLinkDialog = false
            }
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LinkInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Link") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (url.isNotBlank()) onConfirm(url) },
                enabled = url.isNotBlank()
            ) {
                Text("Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun applyStyle(value: TextFieldValue, style: SpanStyle): TextFieldValue {
    val selection = value.selection
    val content = value.annotatedString

    val newAnnotatedString = AnnotatedString.Builder(content).apply {
        addStyle(style, selection.start, selection.end)
    }.toAnnotatedString()

    return value.copy(annotatedString = newAnnotatedString)
}

private fun applyLink(value: TextFieldValue, url: String): TextFieldValue {
    val selection = value.selection
    val content = value.annotatedString

    val newAnnotatedString = AnnotatedString.Builder(content).apply {
        addStyle(
            SpanStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            ),
            selection.start,
            selection.end
        )
        addStringAnnotation("LINK", url, selection.start, selection.end)
    }.toAnnotatedString()

    return value.copy(annotatedString = newAnnotatedString)
}
