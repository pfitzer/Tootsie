package com.pfitzer.tootsie.presentation.screens.search

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.util.openInBrowser
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.EmojiText
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import java.util.Locale

/**
 * Search Screen - search for accounts, posts, and hashtags.
 *
 * @param initialQuery Optional query to pre-fill the search (e.g., from clicking a hashtag)
 * @param onNavigateToProfile Callback to navigate to a user profile
 * @param onNavigateToStatusDetail Callback to navigate to a status detail
 * @param viewModel ViewModel for managing search state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToStatusDetail: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Pre-fill search with initial query if provided (e.g., from hashtag click)
    LaunchedEffect(initialQuery) {
        if (initialQuery != null && initialQuery.isNotBlank() && uiState.query != initialQuery) {
            // If it's a hashtag, filter to show only posts
            if (initialQuery.startsWith("#")) {
                viewModel.onAction(SearchAction.SelectFilter(SearchFilter.STATUSES))
            }
            // Set the query
            viewModel.onAction(SearchAction.UpdateQuery(initialQuery))
        }
    }

    // Opening a result keeps the query in the recent searches
    fun <T> remembering(navigate: (T) -> Unit): (T) -> Unit = { value ->
        viewModel.onAction(SearchAction.RememberQuery)
        navigate(value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.onAction(SearchAction.UpdateQuery(it)) },
                onClearClick = { viewModel.onAction(SearchAction.ClearSearch) },
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.onAction(SearchAction.Submit)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Filter chips
            FilterChips(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.onAction(SearchAction.SelectFilter(it)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.errorMessage != null && !uiState.hasResults -> {
                    ErrorMessage(
                        message = uiState.errorMessage!!,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.isEmpty -> {
                    EmptyResults(query = uiState.query)
                }
                uiState.hasResults -> {
                    SearchResults(
                        uiState = uiState,
                        onAccountClick = remembering(onNavigateToProfile),
                        onStatusClick = remembering(onNavigateToStatusDetail),
                        onHashtagClick = remembering(onNavigateToHashtag),
                        onShowAll = { viewModel.onAction(SearchAction.SelectFilter(it)) },
                        onLoadMore = { viewModel.onAction(SearchAction.LoadMore) },
                        onToggleFollowHashtag = { viewModel.onAction(SearchAction.ToggleFollowHashtag(it)) },
                        onStatusAction = { action -> viewModel.onAction(action) },
                        onImageClick = onNavigateToImageViewer,
                        onVideoClick = onNavigateToVideoPlayer
                    )
                }
                else -> {
                    StartContent(
                        recentSearches = uiState.recentSearches,
                        trendingHashtags = uiState.trendingHashtags,
                        onRecentClick = { query ->
                            viewModel.onAction(SearchAction.UpdateQuery(query))
                            viewModel.onAction(SearchAction.Submit)
                        },
                        onRemoveRecent = { viewModel.onAction(SearchAction.RemoveRecentSearch(it)) },
                        onClearRecent = { viewModel.onAction(SearchAction.ClearRecentSearches) },
                        onHashtagClick = onNavigateToHashtag
                    )
                }
            }
        }
    }
}

/**
 * Search text field.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_search)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

/**
 * Filter chips for result types.
 */
@Composable
private fun FilterChips(
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelRes)) }
            )
        }
    }
}

/**
 * Search results: a preview of every type under "All", one paged type in the other tabs.
 */
@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onAccountClick: (String) -> Unit,
    onStatusClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onShowAll: (SearchFilter) -> Unit,
    onLoadMore: () -> Unit,
    onToggleFollowHashtag: (Hashtag) -> Unit,
    onStatusAction: (SearchAction) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val filter = uiState.selectedFilter
    val isAll = filter == SearchFilter.ALL

    // Load the next page shortly before the end of the list
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, uiState.endReached, filter) {
        if (shouldLoadMore && !isAll && !uiState.endReached) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Accounts section
        if (uiState.accounts.isNotEmpty()) {
            if (isAll) sectionHeader(R.string.search_filter_accounts) { onShowAll(SearchFilter.ACCOUNTS) }
            items(uiState.accounts, key = { "account-${it.id}" }) { account ->
                AccountResultItem(
                    account = account,
                    onClick = { onAccountClick(account.id) }
                )
            }
        }

        // Statuses section
        if (uiState.statuses.isNotEmpty()) {
            if (isAll) sectionHeader(R.string.search_filter_posts) { onShowAll(SearchFilter.STATUSES) }
            items(uiState.statuses, key = { "status-${it.id}" }) { status ->
                StatusCard(
                    status = status,
                    onStatusClick = { onStatusClick(status.id) },
                    onAvatarClick = { onAccountClick(status.account.id) },
                    onMentionClick = onAccountClick,
                    onHashtagClick = onHashtagClick,
                    onLinkClick = { url -> context.openInBrowser(url) },
                    onReplyClick = { onStatusClick(status.id) },
                    onBoostClick = { s ->
                        if (s.reblogged) {
                            onStatusAction(SearchAction.UnboostStatus(s))
                        } else {
                            onStatusAction(SearchAction.BoostStatus(s))
                        }
                    },
                    onFavoriteClick = { s ->
                        if (s.favourited) {
                            onStatusAction(SearchAction.UnfavoriteStatus(s))
                        } else {
                            onStatusAction(SearchAction.FavoriteStatus(s))
                        }
                    },
                    onBookmarkClick = { s ->
                        if (s.bookmarked) {
                            onStatusAction(SearchAction.UnbookmarkStatus(s))
                        } else {
                            onStatusAction(SearchAction.BookmarkStatus(s))
                        }
                    },
                    onVoteClick = { pollId, choices ->
                        onStatusAction(SearchAction.VoteOnPoll(pollId, choices))
                    },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick
                )
            }
        }

        // Hashtags section
        if (uiState.hashtags.isNotEmpty()) {
            if (isAll) sectionHeader(R.string.search_filter_hashtags) { onShowAll(SearchFilter.HASHTAGS) }
            items(uiState.hashtags, key = { "tag-${it.name.lowercase()}" }) { hashtag ->
                HashtagResultItem(
                    hashtag = hashtag,
                    onClick = { onHashtagClick(hashtag.name) },
                    onToggleFollow = { onToggleFollowHashtag(hashtag) }
                )
            }
        }

        if (uiState.isLoadingMore) {
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
    }
}

/**
 * Section title with a "Show all" link to the tab of that type.
 */
private fun LazyListScope.sectionHeader(titleRes: Int, onShowAll: () -> Unit) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onShowAll) {
                Text(stringResource(R.string.search_show_all))
            }
        }
    }
}

/**
 * Account result.
 */
@Composable
private fun AccountResultItem(
    account: Account,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            EmojiText(
                text = account.displayName.ifBlank { account.username },
                emojis = account.emojis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text("@${account.acct}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (account.followersCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.search_followers,
                            account.followersCount,
                            compactNumber(account.followersCount.toLong())
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        leadingContent = {
            AvatarImage(
                url = account.avatar,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Hashtag result with posts of the last week and a follow button.
 */
@Composable
private fun HashtagResultItem(
    hashtag: Hashtag,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    ListItem(
        headlineContent = { Text("#${hashtag.name}") },
        supportingContent = {
            Text(
                pluralStringResource(
                    R.plurals.search_hashtag_posts_week,
                    hashtag.totalUses,
                    compactNumber(hashtag.totalUses.toLong())
                )
            )
        },
        leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
        trailingContent = {
            hashtag.following?.let { following ->
                if (following) {
                    OutlinedButton(onClick = onToggleFollow) { Text(stringResource(R.string.unfollow)) }
                } else {
                    FilledTonalButton(onClick = onToggleFollow) { Text(stringResource(R.string.follow)) }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Short number in the device locale, e.g. 878932 -> "879K" / "879.000" -> "879K"/"878.932". */
private fun compactNumber(value: Long): String =
    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(value)

/**
 * Loading indicator.
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty results message.
 */
@Composable
private fun EmptyResults(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.search_no_results, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.search_no_results_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Before searching: recent searches and trending hashtags, or a hint if there are none.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartContent(
    recentSearches: List<String>,
    trendingHashtags: List<String>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onHashtagClick: (String) -> Unit
) {
    if (recentSearches.isEmpty() && trendingHashtags.isEmpty()) {
        EmptyState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearRecent) {
                        Text(stringResource(R.string.search_recent_clear))
                    }
                }
            }
            items(recentSearches, key = { "recent-$it" }) { query ->
                ListItem(
                    headlineContent = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveRecent(query) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_recent_remove, query)
                            )
                        }
                    },
                    modifier = Modifier.clickable { onRecentClick(query) }
                )
            }
        }

        if (trendingHashtags.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_trending_hashtags),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    trendingHashtags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onHashtagClick(tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state (no search yet).
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.search_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.search_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
