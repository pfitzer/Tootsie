package com.pfitzer.tootsie.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.pfitzer.tootsie.presentation.icons.CustomIcons
import com.pfitzer.tootsie.presentation.icons.Tags
import com.pfitzer.tootsie.presentation.icons.TrendingUp
import com.pfitzer.tootsie.presentation.icons.Link
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineType
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.presentation.components.timeline.TimelineFilterDialog
import com.pfitzer.tootsie.presentation.icons.Hash
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Home Screen - displays the user's home timeline.
 *
 * @param onNavigateToCompose Callback to navigate to compose screen (new post)
 * @param onNavigateToReply Callback to navigate to compose screen for reply with status ID
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToHashtag Callback to navigate to hashtag timeline
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param onNavigateToBookmarks Callback to navigate to bookmarks screen
 * @param onNavigateToFavorites Callback to navigate to favorites screen
 * @param onNavigateToLists Callback to navigate to lists screen
 * @param onNavigateToFollowedTags Callback to navigate to followed tags screen
 * @param onNavigateToTrendingHashtags Callback to navigate to trending hashtags screen
 * @param onNavigateToTrendingPosts Callback to navigate to trending posts screen
 * @param onNavigateToTrendingLinks Callback to navigate to trending links screen
 * @param viewModel ViewModel for managing home screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCompose: () -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    onNavigateToFollowedTags: () -> Unit = {},
    onNavigateToTrendingHashtags: () -> Unit = {},
    onNavigateToTrendingPosts: () -> Unit = {},
    onNavigateToTrendingLinks: () -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTimelineType by viewModel.activeTimelineType.collectAsStateWithLifecycle()
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
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_bookmarks)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToBookmarks()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_favorites)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFavorites()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_lists)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToLists()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(CustomIcons.Tags, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_followed_tags)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFollowedTags()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(CustomIcons.Hash, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_trending_hashtags)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrendingHashtags()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(CustomIcons.TrendingUp, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_trending_statuses)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrendingPosts()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(CustomIcons.Link, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_trending_links)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrendingLinks()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_settings)) },
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
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.cd_menu)
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
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = if (timelineFilter.hasActiveFilters) {
                                            stringResource(R.string.cd_filter_applied)
                                        } else {
                                            stringResource(R.string.cd_no_filter_applied)
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
                    PrimaryTabRow(
                        selectedTabIndex = when (activeTimelineType) {
                            TimelineType.HOME -> 0
                            TimelineType.PUBLIC_LOCAL -> 1
                            TimelineType.PUBLIC_FEDERATED -> 2
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Tab(
                            selected = activeTimelineType == TimelineType.HOME,
                            onClick = { viewModel.onAction(HomeAction.ChangeTimelineType(TimelineType.HOME)) },
                            text = { Text(stringResource(R.string.home_timeline)) }
                        )
                        Tab(
                            selected = activeTimelineType == TimelineType.PUBLIC_LOCAL,
                            onClick = { viewModel.onAction(HomeAction.ChangeTimelineType(TimelineType.PUBLIC_LOCAL)) },
                            text = { Text(stringResource(R.string.local_timeline)) }
                        )
                        Tab(
                            selected = activeTimelineType == TimelineType.PUBLIC_FEDERATED,
                            onClick = { viewModel.onAction(HomeAction.ChangeTimelineType(TimelineType.PUBLIC_FEDERATED)) },
                            text = { Text(stringResource(R.string.federated_timeline)) }
                        )
                    }
                }
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCompose,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_compose_post)
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
                    // Navigate to hashtag timeline
                    onNavigateToHashtag(tagName)
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
                onImageClick = onNavigateToImageViewer,
                onVideoClick = onNavigateToVideoPlayer,
                onVoteClick = { pollId, choices ->
                    viewModel.onAction(HomeAction.VoteOnPoll(pollId, choices))
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
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    onVoteClick: (String, List<Int>) -> Unit,
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
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onVoteClick = onVoteClick,
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
            Text(stringResource(R.string.retry))
        }
    }
}
