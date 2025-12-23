package com.pfitzer.tootsie.presentation.screens.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage

/**
 * Compose Screen - for creating new posts.
 *
 * @param onNavigateBack Callback to navigate back
 * @param viewModel ViewModel for managing compose state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back when post is successful
    LaunchedEffect(uiState.isPosted) {
        if (uiState.isPosted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isReply) "Reply" else "Compose") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Post button
                    TextButton(
                        onClick = { viewModel.onAction(ComposeAction.Post) },
                        enabled = uiState.canPost
                    ) {
                        Text(
                            text = if (uiState.isPosting) "Posting..." else "Post",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Reply context (if replying)
            if (uiState.isReply && uiState.replyToAccount != null) {
                Text(
                    text = "Replying to @${uiState.replyToAccount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Content warning (if enabled)
            if (uiState.showContentWarning) {
                OutlinedTextField(
                    value = uiState.contentWarning,
                    onValueChange = {
                        viewModel.onAction(ComposeAction.UpdateContentWarning(it))
                    },
                    label = { Text("Content Warning") },
                    placeholder = { Text("Warning text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )
            }

            // Main content field
            OutlinedTextField(
                value = uiState.content,
                onValueChange = {
                    viewModel.onAction(ComposeAction.UpdateContent(it))
                },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Character counter
            Text(
                text = "${uiState.characterCount} / 500",
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.characterCount > 500) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Compose options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visibility selector
                VisibilityDropdown(
                    selectedVisibility = uiState.visibility,
                    onVisibilitySelected = {
                        viewModel.onAction(ComposeAction.UpdateVisibility(it))
                    }
                )

                // Content warning toggle
                IconButton(
                    onClick = { viewModel.onAction(ComposeAction.ToggleContentWarning) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Content warning",
                        tint = if (uiState.showContentWarning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessage(message = error)
            }

            // Loading indicator
            if (uiState.isPosting) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Dropdown for selecting post visibility.
 */
@Composable
private fun VisibilityDropdown(
    selectedVisibility: StatusVisibility,
    onVisibilitySelected: (StatusVisibility) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedVisibility.displayName)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            StatusVisibility.entries.forEach { visibility ->
                DropdownMenuItem(
                    text = { Text(visibility.displayName) },
                    onClick = {
                        onVisibilitySelected(visibility)
                        expanded = false
                    }
                )
            }
        }
    }
}
