package com.pfitzer.tootsie.presentation.screens.followedtags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import com.pfitzer.tootsie.presentation.icons.CustomIcons
import com.pfitzer.tootsie.presentation.icons.Tags
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.model.FollowedTagSort
import com.pfitzer.tootsie.presentation.components.common.SortMenu
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage

/**
 * Followed Tags Screen - displays hashtags followed by the user.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToHashtag Callback to navigate to hashtag timeline
 * @param viewModel ViewModel for managing followed tags screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedTagsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    viewModel: FollowedTagsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Scroll to top only on explicit refresh or follow
    LaunchedEffect(uiState.scrollToTop) {
        if (uiState.scrollToTop) {
            listState.scrollToItem(0)
            viewModel.onAction(FollowedTagsAction.ScrollToTopConsumed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.followed_tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    SortMenu(
                        options = FollowedTagSort.entries,
                        selected = uiState.sort,
                        label = { sort ->
                            when (sort) {
                                FollowedTagSort.ALPHABETICAL -> R.string.sort_alphabetical
                                FollowedTagSort.ACTIVITY -> R.string.sort_activity
                            }
                        },
                        onSelect = { viewModel.onAction(FollowedTagsAction.SelectSort(it)) }
                    )
                    IconButton(onClick = { viewModel.onAction(FollowedTagsAction.ShowFollowDialog) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.follow_tag)
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
                onRefresh = { viewModel.onAction(FollowedTagsAction.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.followedTags.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.followedTags.isEmpty() && !uiState.isLoading -> {
                        EmptyFollowedTags(
                            onFollow = { viewModel.onAction(FollowedTagsAction.ShowFollowDialog) }
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.sortedTags,
                                key = { it.name }
                            ) { tag ->
                                FollowedTagItem(
                                    tag = tag,
                                    onTagClick = { onNavigateToHashtag(tag.name) },
                                    onUnfollowClick = {
                                        viewModel.onAction(FollowedTagsAction.RequestUnfollowTag(tag))
                                    }
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

            // Unfollow Confirmation Dialog
            uiState.showUnfollowConfirmation?.let { tag ->
                AlertDialog(
                    onDismissRequest = { viewModel.onAction(FollowedTagsAction.DismissUnfollowConfirmation) },
                    title = { Text(stringResource(R.string.unfollow_tag_confirmation_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.unfollow_tag_confirmation_message,
                                tag.name
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onAction(FollowedTagsAction.UnfollowTag(tag)) }
                        ) {
                            Text(stringResource(R.string.unfollow))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.onAction(FollowedTagsAction.DismissUnfollowConfirmation) }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            // Follow Tag Dialog
            if (uiState.showFollowDialog) {
                var tagName by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { viewModel.onAction(FollowedTagsAction.DismissFollowDialog) },
                    title = { Text(stringResource(R.string.follow_tag)) },
                    text = {
                        OutlinedTextField(
                            value = tagName,
                            onValueChange = { tagName = it },
                            label = { Text(stringResource(R.string.hashtag_name)) },
                            placeholder = { Text("hashtag") },
                            singleLine = true,
                            prefix = { Text("#") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onAction(FollowedTagsAction.FollowTag(tagName)) },
                            enabled = tagName.isNotBlank()
                        ) {
                            Text(stringResource(R.string.follow))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.onAction(FollowedTagsAction.DismissFollowDialog) }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

/**
 * Followed tag with its posts of the last 7 days and an unfollow button.
 */
@Composable
private fun FollowedTagItem(
    tag: FollowedTag,
    onTagClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text("#${tag.name}") },
        supportingContent = {
            Text(pluralStringResource(R.plurals.search_hashtag_posts_week, tag.totalUses, tag.totalUses.toString()))
        },
        leadingContent = {
            Icon(
                imageVector = CustomIcons.Tags,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            TextButton(onClick = onUnfollowClick) {
                Text(stringResource(R.string.unfollow))
            }
        },
        modifier = Modifier.clickable(onClick = onTagClick)
    )
}

@Composable
private fun EmptyFollowedTags(onFollow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = CustomIcons.Tags,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_followed_tags),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.followed_tags_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onFollow) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.follow_tag))
        }
    }
}
