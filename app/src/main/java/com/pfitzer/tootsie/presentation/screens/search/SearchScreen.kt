package com.pfitzer.tootsie.presentation.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard

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
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()

    // Pre-fill search with initial query if provided (e.g., from hashtag click)
    androidx.compose.runtime.LaunchedEffect(initialQuery) {
        if (initialQuery != null && initialQuery.isNotBlank() && uiState.query != initialQuery) {
            // If it's a hashtag, filter to show only posts
            if (initialQuery.startsWith("#")) {
                viewModel.onAction(SearchAction.SelectFilter(SearchFilter.STATUSES))
            }
            // Set the query
            viewModel.onAction(SearchAction.UpdateQuery(initialQuery))
        }
    }

    // Handle hashtag clicks by searching for posts with that hashtag
    val onHashtagClick: (String) -> Unit = { hashtagName ->
        // First set filter to STATUSES to show only posts
        viewModel.onAction(SearchAction.SelectFilter(SearchFilter.STATUSES))
        // Then update query to search for the hashtag
        viewModel.onAction(SearchAction.UpdateQuery("#$hashtagName"))
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
                uiState.errorMessage != null -> {
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
                        onAccountClick = onNavigateToProfile,
                        onStatusClick = onNavigateToStatusDetail,
                        onHashtagClick = onHashtagClick,
                        onStatusAction = { _, _ -> /* TODO: Handle status actions */ },
                        onImageClick = onNavigateToImageViewer,
                        onVideoClick = onNavigateToVideoPlayer
                    )
                }
                else -> {
                    EmptyState()
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

/**
 * Search results display.
 */
@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onAccountClick: (String) -> Unit,
    onStatusClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onStatusAction: (String, String) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Accounts section
        if (uiState.accounts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_section_accounts, uiState.accounts.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(uiState.accounts) { account ->
                AccountResultCard(
                    account = account,
                    onClick = { onAccountClick(account.id) }
                )
            }
        }

        // Statuses section
        if (uiState.statuses.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_section_posts, uiState.statuses.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = if (uiState.accounts.isNotEmpty()) 16.dp else 0.dp)
                )
            }
            items(uiState.statuses) { status ->
                StatusCard(
                    status = status,
                    onStatusClick = { onStatusClick(status.id) },
                    onAvatarClick = { onAccountClick(status.account.id) },
                    onReplyClick = { /* TODO */ },
                    onBoostClick = { /* TODO */ },
                    onFavoriteClick = { /* TODO */ },
                    onBookmarkClick = { /* TODO */ },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick
                )
            }
        }

        // Hashtags section
        if (uiState.hashtags.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_section_hashtags, uiState.hashtags.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        top = if (uiState.accounts.isNotEmpty() || uiState.statuses.isNotEmpty()) 16.dp else 0.dp
                    )
                )
            }
            items(uiState.hashtags) { hashtag ->
                HashtagCard(
                    hashtag = hashtag,
                    onClick = { onHashtagClick(hashtag.name) }
                )
            }
        }
    }
}

/**
 * Account result card.
 */
@Composable
private fun AccountResultCard(
    account: com.pfitzer.tootsie.domain.model.Account,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            com.pfitzer.tootsie.presentation.components.common.AvatarImage(
                url = account.avatar,
                contentDescription = "${account.displayName} avatar",
                modifier = Modifier.size(48.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${account.acct}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.followersCount > 0) {
                    Text(
                        text = stringResource(R.string.search_followers_count, account.followersCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Hashtag result card.
 */
@Composable
private fun HashtagCard(
    hashtag: com.pfitzer.tootsie.domain.model.Hashtag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${hashtag.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hashtag.totalUses > 0) {
                    Text(
                        text = stringResource(R.string.search_hashtag_uses, hashtag.totalUses),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
