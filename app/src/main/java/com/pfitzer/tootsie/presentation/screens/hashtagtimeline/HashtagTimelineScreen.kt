package com.pfitzer.tootsie.presentation.screens.hashtagtimeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Hashtag Timeline Screen - displays statuses containing a specific hashtag.
 *
 * @param hashtag The hashtag name (without the # symbol)
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToHashtag Callback to navigate to another hashtag timeline
 * @param viewModel ViewModel for managing hashtag timeline screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagTimelineScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: HashtagTimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val hashtagTimelinePagingItems = viewModel.hashtagTimelinePagingData.collectAsLazyPagingItems()
    var showUnfollowConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$hashtag") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    // Active Account Avatar
                    activeAccount?.let { account ->
                        IconButton(onClick = { onNavigateToProfile(account.id) }) {
                            AvatarImage(
                                url = account.avatar,
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Follow/Unfollow button
                    if (uiState.isProcessingFollow) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (uiState.isFollowing) {
                                    showUnfollowConfirmation = true
                                } else {
                                    viewModel.onAction(HashtagTimelineAction.FollowTag)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isFollowing) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = if (uiState.isFollowing) {
                                    stringResource(R.string.unfollow_tag)
                                } else {
                                    stringResource(R.string.follow_tag)
                                }
                            )
                        }
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
            HashtagTimelineList(
                hashtagTimelinePagingItems = hashtagTimelinePagingItems,
                onStatusClick = onNavigateToStatus,
                onAvatarClick = onNavigateToProfile,
                onMentionClick = onNavigateToProfile,
                onHashtagClick = { tagName ->
                    onNavigateToHashtag(tagName)
                },
                onFavoriteClick = { status ->
                    if (status.favourited) {
                        viewModel.onAction(HashtagTimelineAction.UnfavoriteStatus(status))
                    } else {
                        viewModel.onAction(HashtagTimelineAction.FavoriteStatus(status))
                    }
                },
                onBoostClick = { status ->
                    if (status.reblogged) {
                        viewModel.onAction(HashtagTimelineAction.UnboostStatus(status))
                    } else {
                        viewModel.onAction(HashtagTimelineAction.BoostStatus(status))
                    }
                },
                onBookmarkClick = { status ->
                    if (status.bookmarked) {
                        viewModel.onAction(HashtagTimelineAction.UnbookmarkStatus(status))
                    } else {
                        viewModel.onAction(HashtagTimelineAction.BookmarkStatus(status))
                    }
                },
                onReplyClick = { status ->
                    onNavigateToReply(status.id)
                },
                onImageClick = onNavigateToImageViewer,
                onVideoClick = onNavigateToVideoPlayer,
                onVoteClick = { pollId, choices ->
                    viewModel.onAction(HashtagTimelineAction.VoteOnPoll(pollId, choices))
                }
            )

            // Error message
            uiState.errorMessage?.let { error ->
                ErrorMessage(
                    message = error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            // Unfollow Confirmation Dialog
            if (showUnfollowConfirmation) {
                AlertDialog(
                    onDismissRequest = { showUnfollowConfirmation = false },
                    title = { Text(stringResource(R.string.unfollow_tag_confirmation_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.unfollow_tag_confirmation_message,
                                hashtag
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.onAction(HashtagTimelineAction.UnfollowTag)
                                showUnfollowConfirmation = false
                            }
                        ) {
                            Text(stringResource(R.string.unfollow))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showUnfollowConfirmation = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

/**
 * Hashtag timeline with pagination.
 */
@Composable
private fun HashtagTimelineList(
    hashtagTimelinePagingItems: LazyPagingItems<Status>,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    onVoteClick: (String, List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = hashtagTimelinePagingItems.itemCount,
            key = hashtagTimelinePagingItems.itemKey { it.id }
        ) { index ->
            val status = hashtagTimelinePagingItems[index]
            status?.let { currentStatus ->
                StatusCard(
                    status = currentStatus,
                    onStatusClick = { onStatusClick(currentStatus.id) },
                    onAvatarClick = { onAvatarClick(currentStatus.account.id) },
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick,
                    onFavoriteClick = { onFavoriteClick(currentStatus) },
                    onBoostClick = { onBoostClick(currentStatus) },
                    onBookmarkClick = { onBookmarkClick(currentStatus) },
                    onReplyClick = { onReplyClick(currentStatus) },
                    onLinkClick = { url -> context.openInBrowser(url) },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onVoteClick = onVoteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Loading state
        when {
            hashtagTimelinePagingItems.loadState.refresh is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            hashtagTimelinePagingItems.loadState.append is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            hashtagTimelinePagingItems.loadState.refresh is LoadState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.hashtag_timeline_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            hashtagTimelinePagingItems.itemCount == 0 -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.hashtag_timeline_no_posts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
