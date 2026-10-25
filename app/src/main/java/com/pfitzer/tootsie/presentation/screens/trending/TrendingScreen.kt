package com.pfitzer.tootsie.presentation.screens.trending

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TrendingTagSort
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.common.SortMenu
import com.pfitzer.tootsie.presentation.components.status.ShareBottomSheet
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.presentation.screens.trending.components.TrendingHashtagCard
import com.pfitzer.tootsie.presentation.screens.trending.components.TrendingLinkCard
import com.pfitzer.tootsie.presentation.screens.trendinglinks.TrendingLinksAction
import com.pfitzer.tootsie.presentation.screens.trendinglinks.TrendingLinksViewModel
import com.pfitzer.tootsie.presentation.screens.trendingposts.TrendingPostsAction
import com.pfitzer.tootsie.presentation.screens.trendingposts.TrendingPostsViewModel
import com.pfitzer.tootsie.util.openInBrowser
import kotlinx.coroutines.launch

enum class TrendingTab(val titleResId: Int) {
    POSTS(R.string.trending_posts_title),
    HASHTAGS(R.string.trending_hashtags_title),
    LINKS(R.string.trending_links_title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReply: (String) -> Unit = {},
    onNavigateToStatus: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    postsViewModel: TrendingPostsViewModel = hiltViewModel(),
    hashtagsViewModel: TrendingHashtagsViewModel = hiltViewModel(),
    linksViewModel: TrendingLinksViewModel = hiltViewModel()
) {
    val tabs = TrendingTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf<Status?>(null) }
    val hashtagsUiState by hashtagsViewModel.uiState.collectAsStateWithLifecycle()

    showShareMenu?.let { status ->
        ShareBottomSheet(
            status = status,
            onDismissRequest = { showShareMenu = null }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.trending_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        // Only the hashtags can be sorted
                        if (tabs[pagerState.currentPage] == TrendingTab.HASHTAGS) {
                            SortMenu(
                                options = TrendingTagSort.entries,
                                selected = hashtagsUiState.sort,
                                label = { sort ->
                                    when (sort) {
                                        TrendingTagSort.TRENDING -> R.string.sort_trending
                                        TrendingTagSort.ALPHABETICAL -> R.string.sort_alphabetical
                                        TrendingTagSort.ACTIVITY -> R.string.sort_activity
                                    }
                                },
                                onSelect = { hashtagsViewModel.onAction(TrendingHashtagsAction.SelectSort(it)) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = stringResource(tab.titleResId),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (tabs[page]) {
                TrendingTab.POSTS -> {
                    TrendingPostsContent(
                        viewModel = postsViewModel,
                        onNavigateToStatus = onNavigateToStatus,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToHashtag = onNavigateToHashtag,
                        onNavigateToReply = onNavigateToReply,
                        onNavigateToImageViewer = onNavigateToImageViewer,
                        onNavigateToVideoPlayer = onNavigateToVideoPlayer,
                        onShareClick = { showShareMenu = it }
                    )
                }
                TrendingTab.HASHTAGS -> {
                    TrendingHashtagsContent(
                        viewModel = hashtagsViewModel,
                        onNavigateToHashtag = onNavigateToHashtag
                    )
                }
                TrendingTab.LINKS -> {
                    TrendingLinksContent(
                        viewModel = linksViewModel,
                        onNavigateToLink = { context.openInBrowser(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingPostsContent(
    viewModel: TrendingPostsViewModel,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit,
    onNavigateToReply: (String) -> Unit,
    onNavigateToImageViewer: (String, String?) -> Unit,
    onNavigateToVideoPlayer: (String, String?) -> Unit,
    onShareClick: (Status) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Scroll to top on refresh
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.trendingPosts.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.onAction(TrendingPostsAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading && uiState.trendingPosts.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.trendingPosts.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_trending_posts),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = uiState.trendingPosts) { status ->
                    StatusCard(
                        status = status,
                        onStatusClick = { onNavigateToStatus(status.id) },
                        onAvatarClick = { onNavigateToProfile(status.account.id) },
                        onMentionClick = onNavigateToProfile,
                        onHashtagClick = onNavigateToHashtag,
                        onFavoriteClick = {
                            viewModel.onAction(
                                if (status.favourited) TrendingPostsAction.UnfavoriteStatus(status)
                                else TrendingPostsAction.FavoriteStatus(status)
                            )
                        },
                        onBoostClick = {
                            viewModel.onAction(
                                if (status.reblogged) TrendingPostsAction.UnboostStatus(status)
                                else TrendingPostsAction.BoostStatus(status)
                            )
                        },
                        onBookmarkClick = {
                            viewModel.onAction(
                                if (status.bookmarked) TrendingPostsAction.UnbookmarkStatus(status)
                                else TrendingPostsAction.BookmarkStatus(status)
                            )
                        },
                        onShareClick = onShareClick,
                        onReplyClick = { onNavigateToReply(status.id) },
                        onLinkClick = { context.openInBrowser(it) },
                        onVoteClick = { pollId, choices ->
                            viewModel.onAction(TrendingPostsAction.VoteOnPoll(pollId, choices))
                        },
                        onImageClick = onNavigateToImageViewer,
                        onVideoClick = onNavigateToVideoPlayer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        uiState.errorMessage?.let {
            ErrorMessage(message = it, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingHashtagsContent(
    viewModel: TrendingHashtagsViewModel,
    onNavigateToHashtag: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Scroll to top on refresh
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.trendingTags.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // Scroll to top after the sort order changed
    LaunchedEffect(uiState.scrollToTop) {
        if (uiState.scrollToTop) {
            listState.scrollToItem(0)
            viewModel.onAction(TrendingHashtagsAction.ScrollToTopConsumed)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.onAction(TrendingHashtagsAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading && uiState.trendingTags.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.trendingTags.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_trending_hashtags),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = uiState.sortedTags) { tag ->
                    TrendingHashtagCard(
                        trendingTag = tag,
                        onClick = { onNavigateToHashtag(tag.name) }
                    )
                }
            }
        }
        uiState.errorMessage?.let {
            ErrorMessage(message = it, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingLinksContent(
    viewModel: TrendingLinksViewModel,
    onNavigateToLink: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Scroll to top on refresh
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.trendingLinks.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.onAction(TrendingLinksAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading && uiState.trendingLinks.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.trendingLinks.isEmpty()) {
            Text(
                text = stringResource(R.string.error_failed_to_load), // Use a better string if available
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = uiState.trendingLinks) { link ->
                    TrendingLinkCard(
                        trendingLink = link,
                        onClick = { onNavigateToLink(link.url) }
                    )
                }
            }
        }
        uiState.errorMessage?.let {
            ErrorMessage(message = it, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}
