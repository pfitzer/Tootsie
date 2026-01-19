package com.pfitzer.tootsie.presentation.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Notification
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.notification.NotificationCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Notifications Screen - displays user notifications (follows, favorites, boosts, mentions).
 *
 * @param onNavigateToStatus Callback to navigate to status detail
 * @param onNavigateToProfile Callback to navigate to profile
 * @param onNavigateToSearch Callback to navigate to search with hashtag
 * @param viewModel ViewModel for managing notifications state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToStatus: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val notificationsPagingItems = viewModel.notificationsPagingData.collectAsLazyPagingItems()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
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

                    // Clear all button
                    IconButton(
                        onClick = { viewModel.onAction(NotificationsAction.ClearAll) },
                        enabled = !uiState.isClearingAll && notificationsPagingItems.itemCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_all_drafts)
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
        PullToRefreshBox(
            isRefreshing = notificationsPagingItems.loadState.refresh is LoadState.Loading,
            onRefresh = {
                viewModel.onAction(NotificationsAction.Refresh)
                notificationsPagingItems.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Notifications
                items(notificationsPagingItems.itemCount) { index ->
                    notificationsPagingItems[index]?.let { notification ->
                        SwipeToDismissNotification(
                            notification = notification,
                            onDismiss = {
                                viewModel.onAction(NotificationsAction.DismissNotification(notification.id))
                            }
                        ) {
                            NotificationCard(
                                notification = notification,
                                onNotificationClick = { clickedNotification ->
                                    // Navigate to status detail if notification has a status
                                    // If the status is a reply, navigate to the original status instead
                                    // Otherwise navigate to the account's profile (for follow notifications)
                                    if (clickedNotification.status != null) {
                                        val targetStatusId = clickedNotification.status.inReplyToId
                                            ?: clickedNotification.status.id
                                        onNavigateToStatus(targetStatusId)
                                    } else {
                                        onNavigateToProfile(clickedNotification.account.id)
                                    }
                                },
                                onAvatarClick = { accountId ->
                                    onNavigateToProfile(accountId)
                                },
                                onMentionClick = { accountId ->
                                    onNavigateToProfile(accountId)
                                },
                                onHashtagClick = { tag ->
                                    onNavigateToSearch(tag)
                                },
                                onLinkClick = { url ->
                                    context.openInBrowser(url)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Loading states
                if (notificationsPagingItems.loadState.append is LoadState.Loading) {
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

                // Empty state
                if (notificationsPagingItems.loadState.refresh is LoadState.NotLoading &&
                    notificationsPagingItems.itemCount == 0
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error message overlay
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onAction(NotificationsAction.DismissError) }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun SwipeToDismissNotification(
    notification: Notification,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_draft),
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = { content() }
    )
}
