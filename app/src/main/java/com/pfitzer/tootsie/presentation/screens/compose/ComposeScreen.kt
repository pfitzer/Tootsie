package com.pfitzer.tootsie.presentation.screens.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.compose.AltTextDialog
import com.pfitzer.tootsie.presentation.components.compose.MediaPreviewGrid
import com.pfitzer.tootsie.util.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compose Screen - for creating new posts.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToDrafts Callback to navigate to drafts screen
 * @param viewModel ViewModel for managing compose state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDrafts: () -> Unit = {},
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Alt text dialog state
    var showAltTextDialog by remember { mutableStateOf(false) }
    var selectedMediaForAltText by remember { mutableStateOf<MediaAttachment?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                // Convert URI to File in background thread
                val file = withContext(Dispatchers.IO) {
                    it.toFile(context)
                }
                file?.let { f ->
                    viewModel.onAction(ComposeAction.AddMedia(f))
                }
            }
        }
    }

    // Navigate back when post is successful
    LaunchedEffect(uiState.isPosted) {
        if (uiState.isPosted) {
            onNavigateBack()
        }
    }

    // Alt text dialog
    if (showAltTextDialog && selectedMediaForAltText != null) {
        AltTextDialog(
            media = selectedMediaForAltText!!,
            onDismiss = {
                showAltTextDialog = false
                selectedMediaForAltText = null
            },
            onSave = { attachmentId, description ->
                viewModel.onAction(ComposeAction.UpdateMediaDescription(attachmentId, description))
            }
        )
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
                    // Drafts button
                    IconButton(onClick = onNavigateToDrafts) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "View drafts"
                        )
                    }

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

            // Media preview grid
            if (uiState.mediaAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MediaPreviewGrid(
                    mediaAttachments = uiState.mediaAttachments,
                    onRemoveMedia = { attachmentId ->
                        viewModel.onAction(ComposeAction.RemoveMedia(attachmentId))
                    },
                    onEditAltText = { media ->
                        selectedMediaForAltText = media
                        showAltTextDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                )
            }

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Visibility selector
                    VisibilityDropdown(
                        selectedVisibility = uiState.visibility,
                        onVisibilitySelected = {
                            viewModel.onAction(ComposeAction.UpdateVisibility(it))
                        }
                    )

                    // Attach media button
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        enabled = uiState.canAddMedia
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Attach media (image or video)",
                            tint = if (uiState.canAddMedia) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }

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
            }

            // Upload progress indicator
            if (uiState.isUploadingMedia) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uploading media...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
