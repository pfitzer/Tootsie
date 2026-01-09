package com.pfitzer.tootsie.presentation.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.paging.compose.collectAsLazyPagingItems
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
    val notificationsPagingItems = viewModel.notificationsPagingData.collectAsLazyPagingItems()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                actions = {
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
        Box(
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
                        NotificationCard(
                            notification = notification,
                            onNotificationClick = { clickedNotification ->
                                // Navigate to status detail if notification has a status
                                // Otherwise navigate to the account's profile (for follow notifications)
                                if (clickedNotification.status != null) {
                                    onNavigateToStatus(clickedNotification.status.id)
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

                // Loading states
                when (notificationsPagingItems.loadState.refresh) {
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
                                message = stringResource(R.string.error_load_notifications),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    else -> {}
                }

                when (notificationsPagingItems.loadState.append) {
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
                    else -> {}
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
