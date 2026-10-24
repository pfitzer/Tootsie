package com.pfitzer.tootsie.presentation.screens.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Quote
import com.pfitzer.tootsie.domain.model.QuoteState
import com.pfitzer.tootsie.presentation.components.status.QuotedStatusCard
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.compose.AltTextDialog
import com.pfitzer.tootsie.presentation.components.compose.EmojiPickerDialog
import com.pfitzer.tootsie.presentation.components.compose.MediaPreviewGrid
import com.pfitzer.tootsie.presentation.components.compose.ComposePollComponent
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

    // Emoji picker dialog state
    var showEmojiPicker by remember { mutableStateOf(false) }

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

    // Emoji picker dialog
    if (showEmojiPicker) {
        EmojiPickerDialog(
            emojis = uiState.customEmojis,
            onEmojiSelected = { shortcode ->
                // Insert emoji shortcode into content
                val currentContent = uiState.content
                val newContent = "$currentContent:$shortcode:"
                viewModel.onAction(ComposeAction.UpdateContent(newContent))
            },
            onDismiss = { showEmojiPicker = false },
            isLoading = uiState.isLoadingEmojis
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(
                        when {
                            uiState.isReply -> R.string.compose_title_reply
                            uiState.isQuote -> R.string.compose_title_quote
                            else -> R.string.compose
                        }
                    ))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    // Drafts button
                    IconButton(onClick = onNavigateToDrafts) {
                        Icon(
                            imageVector = Icons.Default.Drafts,
                            contentDescription = stringResource(R.string.cd_view_drafts)
                        )
                    }

                    // Post button
                    TextButton(
                        onClick = { viewModel.onAction(ComposeAction.Post) },
                        enabled = uiState.canPost
                    ) {
                        Text(
                            text = stringResource(
                                if (uiState.isPosting) R.string.compose_button_posting
                                else R.string.compose_button_post
                            ),
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
            uiState.replyToAccount?.let { account ->
                if (uiState.isReply) {
                    Text(
                        text = stringResource(R.string.compose_replying_to, account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Content warning (if enabled)
            if (uiState.showContentWarning) {
                OutlinedTextField(
                    value = uiState.contentWarning,
                    onValueChange = {
                        viewModel.onAction(ComposeAction.UpdateContentWarning(it))
                    },
                    label = { Text(stringResource(R.string.content_warning)) },
                    placeholder = { Text(stringResource(R.string.compose_placeholder_warning)) },
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
                placeholder = { Text(stringResource(R.string.compose_placeholder_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 5
            )

            // Preview of the quoted status
            uiState.quotedStatus?.let { quoted ->
                Spacer(modifier = Modifier.height(16.dp))
                QuotedStatusCard(
                    quote = Quote(state = QuoteState.ACCEPTED, quotedStatus = quoted),
                    onClick = null
                )
            }

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

            // Poll Creator
            uiState.pollConfig?.let { pollConfig ->
                Spacer(modifier = Modifier.height(16.dp))
                ComposePollComponent(
                    pollConfig = pollConfig,
                    onUpdateOption = { index, text ->
                        viewModel.onAction(ComposeAction.UpdatePollOption(index, text))
                    },
                    onAddOption = {
                        viewModel.onAction(ComposeAction.AddPollOption)
                    },
                    onRemoveOption = { index ->
                        viewModel.onAction(ComposeAction.RemovePollOption(index))
                    },
                    onUpdateExpiry = { expiresIn ->
                        viewModel.onAction(ComposeAction.UpdatePollExpiry(expiresIn))
                    },
                    onToggleMultipleChoice = {
                        viewModel.onAction(ComposeAction.TogglePollMultipleChoice)
                    },
                    onToggleHideTotals = {
                        viewModel.onAction(ComposeAction.TogglePollHideTotals)
                    },
                    onRemovePoll = {
                        viewModel.onAction(ComposeAction.RemovePoll)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Character counter
            Text(
                text = stringResource(R.string.compose_character_count, uiState.characterCount),
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
                            contentDescription = stringResource(R.string.cd_attach_media),
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
                            contentDescription = stringResource(R.string.cd_content_warning),
                            tint = if (uiState.showContentWarning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Emoji picker button
                    IconButton(
                        onClick = { showEmojiPicker = true }
                    ) {
                        Text(
                            text = "😊",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Poll button
                    IconButton(
                        onClick = { viewModel.onAction(ComposeAction.AddPoll) },
                        enabled = uiState.canAddPoll
                    ) {
                        Icon(
                            imageVector = Icons.Default.Poll,
                            contentDescription = stringResource(R.string.poll),
                            tint = if (uiState.canAddPoll) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
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
                        text = stringResource(R.string.uploading_media),
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
