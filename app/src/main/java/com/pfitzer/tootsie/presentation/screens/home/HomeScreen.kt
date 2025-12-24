package com.pfitzer.tootsie.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.presentation.components.timeline.TimelineFilterDialog

/**
 * Home Screen - displays the user's home timeline.
 *
 * @param onNavigateToCompose Callback to navigate to compose screen (new post)
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param viewModel ViewModel for managing home screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCompose: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timelineFilter by viewModel.timelineFilter.collectAsStateWithLifecycle()
    val timelinePagingItems = viewModel.timelinePagingData.collectAsLazyPagingItems()

    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter dialog
    if (showFilterDialog) {
        TimelineFilterDialog(
            currentFilter = timelineFilter,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter ->
                viewModel.onAction(HomeAction.UpdateFilter(newFilter))
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    // Filter button with indicator
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (timelineFilter.hasActiveFilters) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Filter timeline"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCompose,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Compose new post"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TimelineList(
                timelinePagingItems = timelinePagingItems,
                onStatusClick = onNavigateToStatus,
                onAvatarClick = onNavigateToProfile,
                onFavoriteClick = { status ->
                    if (status.favourited) {
                        viewModel.onAction(HomeAction.UnfavoriteStatus(status))
                    } else {
                        viewModel.onAction(HomeAction.FavoriteStatus(status))
                    }
                },
                onBoostClick = { status ->
                    if (status.reblogged) {
                        viewModel.onAction(HomeAction.UnboostStatus(status))
                    } else {
                        viewModel.onAction(HomeAction.BoostStatus(status))
                    }
                },
                onBookmarkClick = { status ->
                    if (status.bookmarked) {
                        viewModel.onAction(HomeAction.UnbookmarkStatus(status))
                    } else {
                        viewModel.onAction(HomeAction.BookmarkStatus(status))
                    }
                },
                onReplyClick = { status ->
                    onNavigateToReply(status.id)
                },
                onRefresh = {
                    viewModel.onAction(HomeAction.RefreshTimeline)
                },
                isRefreshing = uiState.isRefreshing
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
 * Timeline list with pull-to-refresh support.
 */
@Composable
private fun TimelineList(
    timelinePagingItems: LazyPagingItems<Status>,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Handle refresh state
        if (timelinePagingItems.loadState.refresh is LoadState.Loading && !isRefreshing) {
            item {
                LoadingIndicator()
            }
        }

        // Timeline items
        items(
            count = timelinePagingItems.itemCount,
            key = timelinePagingItems.itemKey { it.id }
        ) { index ->
            val status = timelinePagingItems[index]
            if (status != null) {
                StatusCard(
                    status = status,
                    onStatusClick = onStatusClick,
                    onAvatarClick = onAvatarClick,
                    onFavoriteClick = onFavoriteClick,
                    onBoostClick = onBoostClick,
                    onReplyClick = onReplyClick,
                    onBookmarkClick = onBookmarkClick,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Handle append state
        if (timelinePagingItems.loadState.append is LoadState.Loading) {
            item {
                LoadingIndicator()
            }
        }

        // Handle errors
        if (timelinePagingItems.loadState.refresh is LoadState.Error) {
            item {
                ErrorState(
                    message = "Failed to load timeline",
                    onRetry = { timelinePagingItems.retry() }
                )
            }
        }

        if (timelinePagingItems.loadState.append is LoadState.Error) {
            item {
                ErrorState(
                    message = "Failed to load more",
                    onRetry = { timelinePagingItems.retry() }
                )
            }
        }
    }
}

/**
 * Loading indicator.
 */
@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state with retry button.
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
