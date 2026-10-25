package com.pfitzer.tootsie.presentation.screens.scheduled

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.compose.ScheduleTimePickerDialog
import com.pfitzer.tootsie.presentation.screens.compose.StatusVisibility
import com.pfitzer.tootsie.presentation.util.LocaleAwareDateFormatter

/**
 * Scheduled Posts Screen - posts the server publishes later.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onEditScheduledPost Opens a scheduled post in the compose screen
 * @param viewModel ViewModel for managing scheduled posts screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledPostsScreen(
    onNavigateBack: () -> Unit,
    onEditScheduledPost: (String) -> Unit,
    viewModel: ScheduledPostsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var postToDelete by remember { mutableStateOf<ScheduledStatus?>(null) }
    var postToReschedule by remember { mutableStateOf<ScheduledStatus?>(null) }

    // Reload when coming back from editing a post
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(ScheduledPostsAction.Load)
        onPauseOrDispose { }
    }

    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text(stringResource(R.string.scheduled_delete_confirm_title)) },
            text = { Text(stringResource(R.string.scheduled_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(ScheduledPostsAction.Delete(post.id))
                    postToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    postToReschedule?.let { post ->
        ScheduleTimePickerDialog(
            initial = post.scheduledAt,
            onConfirm = {
                viewModel.onAction(ScheduledPostsAction.Reschedule(post.id, it))
                postToReschedule = null
            },
            onDismiss = { postToReschedule = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduled_posts_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.scheduledPosts.isNotEmpty(),
            onRefresh = { viewModel.onAction(ScheduledPostsAction.Load) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.scheduledPosts.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.scheduledPosts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_scheduled_posts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.scheduledPosts, key = { it.id }) { post ->
                            ScheduledPostItem(
                                post = post,
                                isProcessing = post.id in uiState.processingIds,
                                onEdit = { onEditScheduledPost(post.id) },
                                onReschedule = { postToReschedule = post },
                                onDelete = { postToDelete = post }
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                ErrorMessage(
                    message = error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Scheduled post: time, content warning, text, attachments, and its actions.
 */
@Composable
private fun ScheduledPostItem(
    post: ScheduledStatus,
    isProcessing: Boolean,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val visibility = StatusVisibility.entries.find { it.value == post.visibility }

    Card(
        onClick = onEdit,
        enabled = !isProcessing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LocaleAwareDateFormatter.formatAbsoluteTime(context, post.scheduledAt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                visibility?.let {
                    Text(
                        text = stringResource(it.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            post.spoilerText?.let { warning ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.text.ifBlank { stringResource(R.string.scheduled_no_text) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (post.mediaAttachments.isNotEmpty() || post.poll != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (post.mediaAttachments.isNotEmpty()) {
                        AttachmentLabel(
                            icon = Icons.Outlined.AttachFile,
                            text = pluralStringResource(
                                R.plurals.scheduled_media_count,
                                post.mediaAttachments.size,
                                post.mediaAttachments.size
                            )
                        )
                    }
                    if (post.poll != null) {
                        AttachmentLabel(icon = Icons.Outlined.BarChart, text = stringResource(R.string.poll))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReschedule, enabled = !isProcessing) {
                    Text(stringResource(R.string.scheduled_change_time))
                }
                TextButton(onClick = onEdit, enabled = !isProcessing) {
                    Text(stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete, enabled = !isProcessing) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
