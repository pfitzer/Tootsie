package com.pfitzer.tootsie.presentation.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.ShareBottomSheet
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Favorites Screen - displays the user's favorited statuses.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToHashtag Callback to navigate to hashtag timeline
 * @param viewModel ViewModel for managing favorites screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoritesPagingItems = viewModel.favoritesPagingData.collectAsLazyPagingItems()
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
                title = { Text(stringResource(R.string.favorites_title)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FavoritesList(
                favoritesPagingItems = favoritesPagingItems,
                onStatusClick = onNavigateToStatus,
                onAvatarClick = onNavigateToProfile,
                onMentionClick = onNavigateToProfile,
                onHashtagClick = onNavigateToHashtag,
                onFavoriteClick = { status ->
                    if (status.favourited) {
                        viewModel.onAction(FavoritesAction.UnfavoriteStatus(status))
                    } else {
                        viewModel.onAction(FavoritesAction.FavoriteStatus(status))
                    }
                },
                onBoostClick = { status ->
                    if (status.reblogged) {
                        viewModel.onAction(FavoritesAction.UnboostStatus(status))
                    } else {
                        viewModel.onAction(FavoritesAction.BoostStatus(status))
                    }
                },
                onBookmarkClick = { status ->
                    if (status.bookmarked) {
                        viewModel.onAction(FavoritesAction.UnbookmarkStatus(status))
                    } else {
                        viewModel.onAction(FavoritesAction.BookmarkStatus(status))
                    }
                },
                onReplyClick = { status ->
                    onNavigateToReply(status.id)
                },
                onShareClick = { status ->
                    showShareMenu = status
                },
                onImageClick = onNavigateToImageViewer,
                onVideoClick = onNavigateToVideoPlayer,
                onVoteClick = { pollId, choices ->
                    viewModel.onAction(FavoritesAction.VoteOnPoll(pollId, choices))
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
        }
    }
}

/**
 * Favorites list with pagination.
 */
@Composable
private fun FavoritesList(
    favoritesPagingItems: LazyPagingItems<Status>,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onShareClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    onVoteClick: (String, List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Scroll to top on refresh
    val loadState = favoritesPagingItems.loadState.refresh
    LaunchedEffect(loadState) {
        if (loadState is LoadState.NotLoading && favoritesPagingItems.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = favoritesPagingItems.itemCount,
            key = favoritesPagingItems.itemKey { it.id }
        ) { index ->
            val status = favoritesPagingItems[index]
            status?.let {
                StatusCard(
                    status = it,
                    onStatusClick = { onStatusClick(status.id) },
                    onAvatarClick = { onAvatarClick(status.account.id) },
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick,
                    onFavoriteClick = { onFavoriteClick(status) },
                    onBoostClick = { onBoostClick(status) },
                    onBookmarkClick = { onBookmarkClick(status) },
                    onReplyClick = { onReplyClick(status) },
                    onShareClick = { onShareClick(status) },
                    onLinkClick = { url ->
                        context.openInBrowser(url)
                    },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onVoteClick = onVoteClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Loading state at the end
        when (favoritesPagingItems.loadState.append) {
            is LoadState.Loading -> {
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
            is LoadState.Error -> {
                item {
                    ErrorMessage(
                        message = stringResource(R.string.error_load_more_favorites),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
            else -> {}
        }

        // Empty state
        if (favoritesPagingItems.loadState.refresh is LoadState.NotLoading
            && favoritesPagingItems.itemCount == 0
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_favorites),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Initial loading state
        if (favoritesPagingItems.loadState.refresh is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Error state
        if (favoritesPagingItems.loadState.refresh is LoadState.Error) {
            item {
                ErrorMessage(
                    message = stringResource(R.string.error_load_favorites),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }
    }
}
