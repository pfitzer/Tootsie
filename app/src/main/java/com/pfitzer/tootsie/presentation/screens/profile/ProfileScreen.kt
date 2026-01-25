package com.pfitzer.tootsie.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.status.RichTextContent
import com.pfitzer.tootsie.presentation.components.status.ShareBottomSheet
import com.pfitzer.tootsie.presentation.components.status.StatusCard
import com.pfitzer.tootsie.util.openInBrowser

/**
 * Profile Screen - displays user profile information.
 *
 * @param accountId The account ID to display (optional for own profile)
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToProfile Callback to navigate to another profile (from mentions)
 * @param onNavigateToHashtag Callback to navigate to hashtag timeline
 * @param viewModel ViewModel for managing profile state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    accountId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    onNavigateToImageViewer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToVideoPlayer: (String, String?) -> Unit = { _, _ -> },
    onNavigateToEditProfile: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val timelinePagingItems = viewModel.timelinePagingData?.collectAsLazyPagingItems()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf<Status?>(null) }

    showShareMenu?.let { status ->
        ShareBottomSheet(
            status = status,
            onDismissRequest = { showShareMenu = null }
        )
    }

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
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                },
                actions = {
                    // Active Account Avatar (only show if not viewing own profile or if we want it consistently everywhere)
                    // If uiState.isOwnProfile is true, we might not need to show the small avatar as the big one is right there.
                    // But for consistency across screens, let's show it if it's NOT the own profile being viewed.
                    if (!uiState.isOwnProfile) {
                        activeAccount?.let { account ->
                            IconButton(onClick = { onNavigateToProfile(account.id) }) {
                                AvatarImage(
                                    url = account.avatar,
                                    contentDescription = stringResource(R.string.profile),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
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
                                },
                                onMentionClick = onNavigateToProfile,
                                onHashtagClick = onNavigateToHashtag,
                                onLinkClick = { url ->
                                    context.openInBrowser(url)
                                },
                                onEditProfileClick = onNavigateToEditProfile
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
                                        onAvatarClick = onNavigateToProfile,
                                        onMentionClick = onNavigateToProfile,
                                        onHashtagClick = onNavigateToHashtag,
                                        onLinkClick = { url ->
                                            context.openInBrowser(url)
                                        },
                                        onImageClick = onNavigateToImageViewer,
                                        onVideoClick = onNavigateToVideoPlayer,
                                        onFavoriteClick = { s ->
                                            if (s.favourited) {
                                                viewModel.onAction(ProfileAction.UnfavoriteStatus(s))
                                            } else {
                                                viewModel.onAction(ProfileAction.FavoriteStatus(s))
                                            }
                                        },
                                        onBoostClick = { s ->
                                            if (s.reblogged) {
                                                viewModel.onAction(ProfileAction.UnboostStatus(s))
                                            } else {
                                                viewModel.onAction(ProfileAction.BoostStatus(s))
                                            }
                                        },
                                        onReplyClick = {}, // TODO: Wire reply navigation
                                        onBookmarkClick = { s ->
                                            if (s.bookmarked) {
                                                viewModel.onAction(ProfileAction.UnbookmarkStatus(s))
                                            } else {
                                                viewModel.onAction(ProfileAction.BookmarkStatus(s))
                                            }
                                        },
                                        onVoteClick = { pollId, choices ->
                                            viewModel.onAction(ProfileAction.VoteOnPoll(pollId, choices))
                                        },
                                        onShareClick = { showShareMenu = it },
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
                                            message = stringResource(R.string.error_load_posts),
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
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onEditProfileClick: () -> Unit,
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
                contentDescription = stringResource(R.string.cd_profile_picture),
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
                            relationship?.following == true -> stringResource(R.string.unfollow)
                            relationship?.requested == true -> stringResource(R.string.notification_requested_follow)
                            else -> stringResource(R.string.follow)
                        }
                    )
                }
            } else {
                // Edit profile button for own profile
                OutlinedButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = stringResource(R.string.edit_profile))
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

        // Bio (note) - render with HTML parsing
        if (account.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            RichTextContent(
                content = account.note,
                mentions = emptyList(), // Account bios don't have structured mention data
                tags = emptyList(),     // Account bios don't have structured tag data
                onMentionClick = onMentionClick,
                onHashtagClick = onHashtagClick,
                onLinkClick = onLinkClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileStat(
                count = account.statusesCount,
                label = stringResource(R.string.profile_posts)
            )
            ProfileStat(
                count = account.followingCount,
                label = stringResource(R.string.profile_following)
            )
            ProfileStat(
                count = account.followersCount,
                label = stringResource(R.string.profile_followers)
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
                        label = { Text(stringResource(R.string.profile_follows_you)) }
                    )
                }
                if (relationship.blocking) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.profile_blocked)) }
                    )
                }
                if (relationship.muting) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.profile_muted)) }
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
