package com.pfitzer.tootsie.presentation.screens.statusdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.ShareBottomSheet
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.ShareUtils
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Status Detail Screen - shows a status with its conversation context.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToStatus Callback to navigate to another status
 * @param onNavigateToProfile Callback to navigate to a profile
 * @param onNavigateToCompose Callback to navigate to compose (for reply) with status ID
 * @param onNavigateToHashtag Callback to navigate to hashtag timeline
 * @param viewModel ViewModel for managing status detail state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCompose: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: StatusDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf<Status?>(null) }

    showShareMenu?.let { status ->
        ShareBottomSheet(
            status = status,
            onDismissRequest = { showShareMenu = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.post)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                        onMentionClick = onNavigateToProfile,
                        onHashtagClick = onNavigateToHashtag,
                        onLinkClick = { url ->
                            context.openInBrowser(url)
                        },
                        onImageClick = onNavigateToImageViewer,
                        onVideoClick = onNavigateToVideoPlayer,
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
                        },
                        onVoteClick = { pollId, choices ->
                            viewModel.onAction(StatusDetailAction.VoteOnPoll(pollId, choices))
                        },
                        onShareClick = { status ->
                            showShareMenu = status
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
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    onReplyClick: (Status) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onShareClick: (Status) -> Unit,
    onVoteClick: (String, List<Int>) -> Unit,
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
                onMentionClick = onMentionClick,
                onHashtagClick = onHashtagClick,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
                onFavoriteClick = onFavoriteClick,
                onBoostClick = onBoostClick,
                onReplyClick = onReplyClick,
                onBookmarkClick = onBookmarkClick,
                onShareClick = onShareClick,
                onVoteClick = onVoteClick,
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
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick,
                    onLinkClick = onLinkClick,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onFavoriteClick = onFavoriteClick,
                    onBoostClick = onBoostClick,
                    onReplyClick = onReplyClick,
                    onBookmarkClick = onBookmarkClick,
                    onShareClick = onShareClick,
                    onVoteClick = onVoteClick
                )
            }
        }

        // Descendants (replies)
        itemsIndexed(descendants) { index, status ->
            ReplyItem(
                status = status,
                isFirst = index == 0,
                isLast = index == descendants.lastIndex,
                onStatusClick = onStatusClick,
                onAvatarClick = onAvatarClick,
                onMentionClick = onMentionClick,
                onHashtagClick = onHashtagClick,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
                onFavoriteClick = onFavoriteClick,
                onBoostClick = onBoostClick,
                onReplyClick = onReplyClick,
                onBookmarkClick = onBookmarkClick,
                onShareClick = onShareClick,
                onVoteClick = onVoteClick
            )
        }

        // Show message if no replies
        if (descendants.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.empty_replies),
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
 * Reply item with continuous left border line.
 */
@Composable
private fun ReplyItem(
    status: Status,
    isFirst: Boolean,
    isLast: Boolean,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    onReplyClick: (Status) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onShareClick: (Status) -> Unit,
    onVoteClick: (String, List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(
                start = 16.dp,
                end = 8.dp,
                top = if (isFirst) 8.dp else 0.dp,
                bottom = if (isLast) 8.dp else 0.dp
            )
    ) {
        // Continuous vertical line
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Status card
        StatusCard(
            status = status,
            onStatusClick = onStatusClick,
            onAvatarClick = onAvatarClick,
            onMentionClick = onMentionClick,
            onHashtagClick = onHashtagClick,
            onLinkClick = onLinkClick,
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
            onFavoriteClick = onFavoriteClick,
            onBoostClick = onBoostClick,
            onReplyClick = onReplyClick,
            onBookmarkClick = onBookmarkClick,
            onShareClick = onShareClick,
            onVoteClick = onVoteClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Helper to create alpha modifier.
 */
private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.graphicsLayer(alpha = alpha)
)
