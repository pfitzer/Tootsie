package com.pfitzer.tootsie.presentation.screens.listtimeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * List Timeline Screen - displays statuses from a specific Mastodon list.
 *
 * @param listName The name of the list (for display in title)
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToSearch Callback to navigate to search with query
 * @param viewModel ViewModel for managing list timeline screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTimelineScreen(
    listName: String,
    onNavigateBack: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: ListTimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listTimelinePagingItems = viewModel.listTimelinePagingData.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
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
            ListTimelineList(
                listTimelinePagingItems = listTimelinePagingItems,
                onStatusClick = onNavigateToStatus,
                onAvatarClick = onNavigateToProfile,
                onMentionClick = onNavigateToProfile,
                onHashtagClick = { tagName ->
                    onNavigateToSearch("#$tagName")
                },
                onFavoriteClick = { status ->
                    if (status.favourited) {
                        viewModel.onAction(ListTimelineAction.UnfavoriteStatus(status))
                    } else {
                        viewModel.onAction(ListTimelineAction.FavoriteStatus(status))
                    }
                },
                onBoostClick = { status ->
                    if (status.reblogged) {
                        viewModel.onAction(ListTimelineAction.UnboostStatus(status))
                    } else {
                        viewModel.onAction(ListTimelineAction.BoostStatus(status))
                    }
                },
                onBookmarkClick = { status ->
                    if (status.bookmarked) {
                        viewModel.onAction(ListTimelineAction.UnbookmarkStatus(status))
                    } else {
                        viewModel.onAction(ListTimelineAction.BookmarkStatus(status))
                    }
                },
                onReplyClick = { status ->
                    onNavigateToReply(status.id)
                },
                onImageClick = onNavigateToImageViewer,
                onVideoClick = onNavigateToVideoPlayer
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
 * List timeline with pagination.
 */
@Composable
private fun ListTimelineList(
    listTimelinePagingItems: LazyPagingItems<Status>,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = listTimelinePagingItems.itemCount,
            key = listTimelinePagingItems.itemKey { it.id }
        ) { index ->
            val status = listTimelinePagingItems[index]
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
                    onLinkClick = { url ->
                        context.openInBrowser(url)
                    },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Loading state at the end
        when (listTimelinePagingItems.loadState.append) {
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
                        message = stringResource(R.string.list_timeline_error_load_more),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
            else -> {}
        }

        // Empty state
        if (listTimelinePagingItems.loadState.refresh is LoadState.NotLoading
            && listTimelinePagingItems.itemCount == 0
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.list_timeline_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Initial loading state
        if (listTimelinePagingItems.loadState.refresh is LoadState.Loading) {
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
        if (listTimelinePagingItems.loadState.refresh is LoadState.Error) {
            item {
                ErrorMessage(
                    message = stringResource(R.string.list_timeline_error_load),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }
    }
}
