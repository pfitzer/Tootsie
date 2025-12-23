package com.pfitzer.tootsie.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.StatusCard

/**
 * Profile Screen - displays user profile information.
 *
 * @param accountId The account ID to display (optional for own profile)
 * @param onNavigateBack Callback to navigate back
 * @param viewModel ViewModel for managing profile state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    accountId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timelinePagingItems = viewModel.timelinePagingData?.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.account?.displayName ?: "Profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.account != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Profile header
                        item {
                            ProfileHeader(
                                account = uiState.account!!,
                                relationship = uiState.relationship,
                                isOwnProfile = uiState.isOwnProfile,
                                isProcessingFollow = uiState.isProcessingFollow,
                                onFollowClick = {
                                    if (uiState.relationship?.following == true) {
                                        viewModel.onAction(ProfileAction.UnfollowAccount)
                                    } else {
                                        viewModel.onAction(ProfileAction.FollowAccount)
                                    }
                                }
                            )
                        }

                        // Divider
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        // Timeline
                        if (timelinePagingItems != null) {
                            items(timelinePagingItems.itemCount) { index ->
                                timelinePagingItems[index]?.let { status ->
                                    StatusCard(
                                        status = status,
                                        onStatusClick = {},
                                        onAvatarClick = {},
                                        onFavoriteClick = {},
                                        onBoostClick = {},
                                        onReplyClick = {},
                                        onBookmarkClick = {},
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Loading states
                            when (timelinePagingItems.loadState.refresh) {
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
                                            message = "Failed to load posts",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }

                            when (timelinePagingItems.loadState.append) {
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
                        }
                    }
                }
                uiState.errorMessage != null -> {
                    ErrorMessage(
                        message = uiState.errorMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Profile header with avatar, stats, and follow button.
 */
@Composable
private fun ProfileHeader(
    account: Account,
    relationship: Relationship?,
    isOwnProfile: Boolean,
    isProcessingFollow: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            AsyncImage(
                model = account.avatar,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Follow button (only for other users)
            if (!isOwnProfile) {
                Button(
                    onClick = onFollowClick,
                    enabled = !isProcessingFollow,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = when {
                            isProcessingFollow -> "..."
                            relationship?.following == true -> "Unfollow"
                            relationship?.requested == true -> "Requested"
                            else -> "Follow"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display name
        Text(
            text = account.displayName.ifBlank { account.username },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Username
        Text(
            text = "@${account.acct}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Bio (note)
        if (account.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileStat(
                count = account.statusesCount,
                label = "Posts"
            )
            ProfileStat(
                count = account.followingCount,
                label = "Following"
            )
            ProfileStat(
                count = account.followersCount,
                label = "Followers"
            )
        }

        // Relationship indicators
        if (relationship != null && !isOwnProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (relationship.followedBy) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Follows you") }
                    )
                }
                if (relationship.blocking) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Blocked") }
                    )
                }
                if (relationship.muting) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Muted") }
                    )
                }
            }
        }
    }
}

/**
 * Profile stat (posts, following, followers).
 */
@Composable
private fun ProfileStat(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
