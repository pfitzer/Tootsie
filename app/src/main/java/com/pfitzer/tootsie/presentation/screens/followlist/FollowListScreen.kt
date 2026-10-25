package com.pfitzer.tootsie.presentation.screens.followlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.FollowListEntry
import com.pfitzer.tootsie.domain.model.AccountListType
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.EmojiText
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage

/**
 * Account list: followers or following of an account, or who favourited or boosted a status.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToProfile Callback to open a profile
 * @param viewModel ViewModel for managing the list state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = viewModel.entries.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(
                                when (viewModel.type) {
                                    AccountListType.FOLLOWERS -> R.string.profile_followers
                                    AccountListType.FOLLOWING -> R.string.profile_following
                                    AccountListType.FAVOURITED_BY -> R.string.favourited_by_title
                                    AccountListType.REBLOGGED_BY -> R.string.reblogged_by_title
                                }
                            )
                        )
                        uiState.account?.let { account ->
                            Text(
                                text = "@${account.acct}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
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
            when {
                entries.loadState.refresh is LoadState.Loading && entries.itemCount == 0 -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                entries.loadState.refresh is LoadState.Error && entries.itemCount == 0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.error_load_follow_list),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { entries.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                entries.itemCount == 0 && entries.loadState.refresh is LoadState.NotLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                if (viewModel.type.ofStatus) R.string.empty_status_account_list else R.string.empty_follow_list
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.isRemoteAccount) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.follow_list_remote_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (uiState.isRemoteAccount) {
                            item(key = "remote_hint") { RemoteHint() }
                        }

                        items(
                            count = entries.itemCount,
                            key = entries.itemKey { it.account.id }
                        ) { index ->
                            entries[index]?.let { entry ->
                                FollowListItem(
                                    entry = entry,
                                    showFollowButton = entry.account.id != uiState.ownAccountId,
                                    isProcessing = entry.account.id in uiState.processingIds,
                                    onClick = { onNavigateToProfile(entry.account.id) },
                                    onToggleFollow = { viewModel.onAction(FollowListAction.ToggleFollow(entry)) }
                                )
                            }
                        }

                        // Loading more indicator
                        if (entries.loadState.append is LoadState.Loading) {
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
            }

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
 * Hint that the list of an account on another server may be incomplete.
 */
@Composable
private fun RemoteHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.follow_list_remote_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Account with its follow button.
 */
@Composable
private fun FollowListItem(
    entry: FollowListEntry,
    showFollowButton: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val account = entry.account
    val relationship = entry.relationship

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
                Text(
                    text = "@${account.acct}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (relationship?.followedBy == true && showFollowButton) {
                    Text(
                        text = stringResource(R.string.profile_follows_you),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        leadingContent = {
            AvatarImage(
                url = account.avatar,
                contentDescription = account.displayName,
                modifier = Modifier.size(48.dp)
            )
        },
        trailingContent = if (showFollowButton && relationship != null) {
            {
                val label = when {
                    relationship.following -> R.string.unfollow
                    relationship.requested -> R.string.follow_requested
                    else -> R.string.follow
                }
                if (relationship.following || relationship.requested) {
                    OutlinedButton(onClick = onToggleFollow, enabled = !isProcessing) {
                        Text(stringResource(label))
                    }
                } else {
                    Button(onClick = onToggleFollow, enabled = !isProcessing) {
                        Text(stringResource(label))
                    }
                }
            }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
