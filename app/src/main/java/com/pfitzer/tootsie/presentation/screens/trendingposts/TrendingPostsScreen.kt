package com.pfitzer.tootsie.presentation.screens.trendingposts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Trending Posts Screen - displays trending posts/statuses.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToReply Callback to navigate to compose screen for reply
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToSearch Callback to navigate to search with query
 * @param viewModel ViewModel for managing trending posts screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingPostsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReply: (String) -> Unit = {},
    onNavigateToStatus: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    viewModel: TrendingPostsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trending_posts_title)) },
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
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.onAction(TrendingPostsAction.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.trendingPosts.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.trendingPosts.isEmpty() && !uiState.isLoading -> {
                        Text(
                            text = stringResource(R.string.empty_trending_posts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.trendingPosts,
                                key = { it.id }
                            ) { status ->
                                StatusCard(
                                    status = status,
                                    onStatusClick = { onNavigateToStatus(status.id) },
                                    onAvatarClick = { onNavigateToProfile(status.account.id) },
                                    onMentionClick = onNavigateToProfile,
                                    onHashtagClick = { tagName ->
                                        onNavigateToSearch("#$tagName")
                                    },
                                    onFavoriteClick = {
                                        if (status.favourited) {
                                            viewModel.onAction(TrendingPostsAction.UnfavoriteStatus(status))
                                        } else {
                                            viewModel.onAction(TrendingPostsAction.FavoriteStatus(status))
                                        }
                                    },
                                    onBoostClick = {
                                        if (status.reblogged) {
                                            viewModel.onAction(TrendingPostsAction.UnboostStatus(status))
                                        } else {
                                            viewModel.onAction(TrendingPostsAction.BoostStatus(status))
                                        }
                                    },
                                    onBookmarkClick = {
                                        if (status.bookmarked) {
                                            viewModel.onAction(TrendingPostsAction.UnbookmarkStatus(status))
                                        } else {
                                            viewModel.onAction(TrendingPostsAction.BookmarkStatus(status))
                                        }
                                    },
                                    onReplyClick = {
                                        onNavigateToReply(status.id)
                                    },
                                    onLinkClick = { url ->
                                        context.openInBrowser(url)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

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
