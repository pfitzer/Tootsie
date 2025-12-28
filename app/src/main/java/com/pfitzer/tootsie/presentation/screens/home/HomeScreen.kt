package com.pfitzer.tootsie.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Home Screen - displays the user's home timeline.
 *
 * @param onNavigateToCompose Callback to navigate to compose screen (new post)
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToSearch Callback to navigate to search with query
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param onNavigateToBookmarks Callback to navigate to bookmarks screen
 * @param onNavigateToFavorites Callback to navigate to favorites screen
 * @param onNavigateToTrendingHashtags Callback to navigate to trending hashtags screen
 * @param viewModel ViewModel for managing home screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCompose: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    onNavigateToTrendingHashtags: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timelineFilter by viewModel.timelineFilter.collectAsStateWithLifecycle()
    val timelinePagingItems = viewModel.timelinePagingData.collectAsLazyPagingItems()

    var showFilterDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tootsie",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Bookmarks") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToBookmarks()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFavorites()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Lists") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToLists()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("Trending") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrendingHashtags()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Home") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu"
                            )
                        }
                    },
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
                onMentionClick = onNavigateToProfile,  // Mentions navigate to profile
                onHashtagClick = { tagName ->
                    // Navigate to search with hashtag query
                    onNavigateToSearch("#$tagName")
                },
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
}

/**
 * Timeline list with pull-to-refresh support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineList(
    timelinePagingItems: LazyPagingItems<Status>,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Determine if we're actually refreshing based on paging state
    val isActuallyRefreshing = timelinePagingItems.loadState.refresh is LoadState.Loading

    PullToRefreshBox(
        isRefreshing = isActuallyRefreshing,
        onRefresh = {
            timelinePagingItems.refresh()
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
        // Initial loading state (when no items are loaded yet)
        if (timelinePagingItems.loadState.refresh is LoadState.Loading && timelinePagingItems.itemCount == 0) {
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
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick,
                    onLinkClick = { url -> context.openInBrowser(url) },
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
        if (timelinePagingItems.loadState.refresh is LoadState.Error && timelinePagingItems.itemCount == 0) {
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
