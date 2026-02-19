package com.pfitzer.tootsie.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
import kotlinx.coroutines.launch

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
    val tabPagingDataMap by viewModel.tabPagingData.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf<Status?>(null) }

    val tabs = ProfileTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

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
                    var headerHeightPx by remember { mutableFloatStateOf(0f) }
                    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                val delta = available.y
                                val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                                val consumed = newOffset - headerOffsetPx
                                headerOffsetPx = newOffset
                                return Offset(0f, consumed)
                            }
                        }
                    }
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        // Scrolling content: spacer + divider + tabs + pager
                        Column(modifier = Modifier.fillMaxSize()) {
                            val spacerHeight = with(density) {
                                (headerHeightPx + headerOffsetPx).coerceAtLeast(0f).toDp()
                            }
                            Spacer(modifier = Modifier.height(spacerHeight))

                            HorizontalDivider()

                            // Tab row
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

                            // Pager content
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val tab = tabs[page]
                                val pagingFlow = tabPagingDataMap[tab]

                                if (pagingFlow != null) {
                                    ProfileTabContent(
                                        tab = tab,
                                        pagingFlow = pagingFlow,
                                        isOwnProfile = uiState.isOwnProfile,
                                        viewModel = viewModel,
                                        onNavigateToProfile = onNavigateToProfile,
                                        onNavigateToHashtag = onNavigateToHashtag,
                                        onNavigateToImageViewer = onNavigateToImageViewer,
                                        onNavigateToVideoPlayer = onNavigateToVideoPlayer,
                                        onShareClick = { showShareMenu = it }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        // Header: overlaid, slides up as user scrolls down
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { headerHeightPx = it.height.toFloat() }
                                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                        ) {
                            ProfileHeader(
                                account = uiState.account!!,
                                relationship = uiState.relationship,
                                isOwnProfile = uiState.isOwnProfile,
                                isProcessingFollow = uiState.isProcessingFollow,
                                isProcessingMute = uiState.isProcessingMute,
                                isProcessingBlock = uiState.isProcessingBlock,
                                onFollowClick = {
                                    if (uiState.relationship?.following == true) {
                                        viewModel.onAction(ProfileAction.UnfollowAccount)
                                    } else {
                                        viewModel.onAction(ProfileAction.FollowAccount)
                                    }
                                },
                                onMuteClick = {
                                    if (uiState.relationship?.muting == true) {
                                        viewModel.onAction(ProfileAction.UnmuteAccount)
                                    } else {
                                        viewModel.onAction(ProfileAction.MuteAccount)
                                    }
                                },
                                onBlockClick = {
                                    if (uiState.relationship?.blocking == true) {
                                        viewModel.onAction(ProfileAction.UnblockAccount)
                                    } else {
                                        viewModel.onAction(ProfileAction.BlockAccount)
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
 * Content for a single profile tab - displays a paged list of statuses.
 */
@Composable
private fun ProfileTabContent(
    tab: ProfileTab,
    pagingFlow: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<Status>>,
    isOwnProfile: Boolean,
    viewModel: ProfileViewModel,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit,
    onNavigateToImageViewer: (String, String?) -> Unit,
    onNavigateToVideoPlayer: (String, String?) -> Unit,
    onShareClick: (Status) -> Unit
) {
    val pagingItems = pagingFlow.collectAsLazyPagingItems()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (pagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LoadState.Error -> {
                ErrorMessage(
                    message = stringResource(R.string.error_load_posts),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            is LoadState.NotLoading -> {
                if (pagingItems.itemCount == 0) {
                    Text(
                        text = stringResource(tab.emptyResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }

        if (pagingItems.itemCount > 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(pagingItems.itemCount) { index ->
                    pagingItems[index]?.let { status ->
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
                            onReplyClick = {},
                            onBookmarkClick = { s ->
                                if (s.bookmarked) {
                                    viewModel.onAction(ProfileAction.UnbookmarkStatus(s))
                                } else {
                                    viewModel.onAction(ProfileAction.BookmarkStatus(s))
                                }
                            },
                            onPinClick = if (isOwnProfile) { { s ->
                                if (s.pinned) {
                                    viewModel.onAction(ProfileAction.UnpinStatus(s))
                                } else {
                                    viewModel.onAction(ProfileAction.PinStatus(s))
                                }
                            } } else null,
                            onVoteClick = { pollId, choices ->
                                viewModel.onAction(ProfileAction.VoteOnPoll(pollId, choices))
                            },
                            onShareClick = onShareClick,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Append loading indicator
                when (pagingItems.loadState.append) {
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
}

/**
 * Profile header with avatar, stats, and follow/mute/block buttons.
 */
@Composable
private fun ProfileHeader(
    account: Account,
    relationship: Relationship?,
    isOwnProfile: Boolean,
    isProcessingFollow: Boolean,
    isProcessingMute: Boolean,
    isProcessingBlock: Boolean,
    onFollowClick: () -> Unit,
    onMuteClick: () -> Unit,
    onBlockClick: () -> Unit,
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

            // Action buttons (only for other users)
            if (!isOwnProfile) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Follow button
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

        // Mute and Block buttons for other users
        if (!isOwnProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mute button
                OutlinedButton(
                    onClick = onMuteClick,
                    enabled = !isProcessingMute,
                    modifier = Modifier.height(32.dp),
                    colors = if (relationship?.muting == true) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(
                        text = when {
                            isProcessingMute -> "..."
                            relationship?.muting == true -> stringResource(R.string.unmute)
                            else -> stringResource(R.string.mute)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Block button
                OutlinedButton(
                    onClick = onBlockClick,
                    enabled = !isProcessingBlock,
                    modifier = Modifier.height(32.dp),
                    colors = if (relationship?.blocking == true) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(
                        text = when {
                            isProcessingBlock -> "..."
                            relationship?.blocking == true -> stringResource(R.string.unblock)
                            else -> stringResource(R.string.block)
                        },
                        style = MaterialTheme.typography.labelMedium
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
