package com.pfitzer.tootsie.presentation.screens.statusdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard

/**
 * Status Detail Screen - shows a status with its conversation context.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToStatus Callback to navigate to another status
 * @param onNavigateToProfile Callback to navigate to a profile
 * @param onNavigateToCompose Callback to navigate to compose (for reply) with status ID
 * @param viewModel ViewModel for managing status detail state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCompose: (String) -> Unit,
    viewModel: StatusDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.status != null -> {
                    ConversationThread(
                        mainStatus = uiState.status!!,
                        ancestors = uiState.ancestors,
                        descendants = uiState.descendants,
                        onStatusClick = onNavigateToStatus,
                        onAvatarClick = onNavigateToProfile,
                        onReplyClick = { status ->
                            onNavigateToCompose(status.id)
                        },
                        onFavoriteClick = { status ->
                            if (status.favourited) {
                                viewModel.onAction(StatusDetailAction.UnfavoriteStatus(status))
                            } else {
                                viewModel.onAction(StatusDetailAction.FavoriteStatus(status))
                            }
                        },
                        onBoostClick = { status ->
                            if (status.reblogged) {
                                viewModel.onAction(StatusDetailAction.UnboostStatus(status))
                            } else {
                                viewModel.onAction(StatusDetailAction.BoostStatus(status))
                            }
                        },
                        onBookmarkClick = { status ->
                            if (status.bookmarked) {
                                viewModel.onAction(StatusDetailAction.UnbookmarkStatus(status))
                            } else {
                                viewModel.onAction(StatusDetailAction.BookmarkStatus(status))
                            }
                        }
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorMessage(
                        message = uiState.errorMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Displays the conversation thread with ancestors, main status, and descendants.
 */
@Composable
private fun ConversationThread(
    mainStatus: Status,
    ancestors: List<Status>,
    descendants: List<Status>,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onReplyClick: (Status) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Ancestors (posts before this one)
        items(ancestors) { status ->
            StatusCard(
                status = status,
                onStatusClick = onStatusClick,
                onAvatarClick = onAvatarClick,
                onFavoriteClick = onFavoriteClick,
                onBoostClick = onBoostClick,
                onReplyClick = onReplyClick,
                onBookmarkClick = onBookmarkClick,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .alpha(0.7f) // Slightly dimmed
            )
        }

        // Main status (highlighted)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                StatusCard(
                    status = mainStatus,
                    onStatusClick = onStatusClick,
                    onAvatarClick = onAvatarClick,
                    onFavoriteClick = onFavoriteClick,
                    onBoostClick = onBoostClick,
                    onReplyClick = onReplyClick,
                    onBookmarkClick = onBookmarkClick
                )
            }
        }

        // Descendants (replies)
        items(descendants) { status ->
            StatusCard(
                status = status,
                onStatusClick = onStatusClick,
                onAvatarClick = onAvatarClick,
                onFavoriteClick = onFavoriteClick,
                onBoostClick = onBoostClick,
                onReplyClick = onReplyClick,
                onBookmarkClick = onBookmarkClick,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .alpha(0.8f) // Slightly dimmed
            )
        }

        // Show message if no replies
        if (descendants.isEmpty()) {
            item {
                Text(
                    text = "No replies yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Helper to create alpha modifier.
 */
private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.graphicsLayer(alpha = alpha)
)
