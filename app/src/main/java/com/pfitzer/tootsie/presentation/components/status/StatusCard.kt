package com.pfitzer.tootsie.presentation.components.status

import com.pfitzer.tootsie.presentation.components.common.EmojiText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.util.LocaleAwareDateFormatter
import com.pfitzer.tootsie.presentation.util.htmlToPlainText

/**
 * Card component that displays a single Mastodon status (post).
 *
 * @param status Status to display
 * @param onStatusClick Callback when status is clicked
 * @param onAvatarClick Callback when avatar is clicked
 * @param onFavoriteClick Callback when favorite button is clicked
 * @param onBoostClick Callback when boost button is clicked
 * @param onReplyClick Callback when reply button is clicked
 * @param onBookmarkClick Callback when bookmark button is clicked
 * @param onPinClick Optional callback when pin button is clicked (only shown if provided, for own posts)
 * @param onReportClick Optional callback when report is selected (only shown if provided, for non-own posts)
 * @param onMentionClick Callback when a mention is clicked (receives accountId)
 * @param onHashtagClick Callback when a hashtag is clicked (receives tag name)
 * @param onLinkClick Callback when a link is clicked (receives URL)
 * @param onImageClick Callback when an image is clicked (receives url, description)
 * @param onVideoClick Callback when a video is clicked (receives url, description)
 * @param modifier Optional modifier
 */
@Composable
fun StatusCard(
    status: Status,
    onStatusClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onFavoriteClick: (Status) -> Unit,
    onBoostClick: (Status) -> Unit,
    onReplyClick: (Status) -> Unit,
    onBookmarkClick: (Status) -> Unit,
    onShareClick: (Status) -> Unit = {},
    onPinClick: ((Status) -> Unit)? = null,
    onReportClick: ((Status) -> Unit)? = null,
    onMentionClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onImageClick: (String, String?) -> Unit = { _, _ -> },
    onVideoClick: (String, String?) -> Unit = { _, _ -> },
    onVoteClick: (String, List<Int>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // If this is a boost/reblog, show the boosted status
    val displayStatus = status.reblog ?: status
    val isReblog = status.reblog != null

    // Track if user has clicked to show filtered content
    var showFilteredContent by remember { mutableStateOf(false) }

    // Content behind a content warning stays hidden until expanded
    val hasContentWarning = displayStatus.spoilerText.isNotBlank()
    var showContentBehindWarning by rememberSaveable(displayStatus.id) { mutableStateOf(false) }
    val showBody = !hasContentWarning || showContentBehindWarning

    // Check if status is filtered with WARN action
    val isFiltered = status.filteredByKeyword != null && !showFilteredContent

    if (isFiltered) {
        // Show placeholder for filtered content
        FilteredContentPlaceholder(
            filterTitle = status.filteredByKeyword,
            onShowContent = { showFilteredContent = true },
            modifier = modifier
        )
    } else {
        // Show normal status content
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onStatusClick(displayStatus.id) },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Boost indicator
                if (isReblog) {
                    BoostIndicator(
                        boosterName = status.account.displayName,
                        boosterEmojis = status.account.emojis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Account header
                StatusHeader(
                    account = displayStatus.account,
                    timestamp = displayStatus.createdAt,
                    isPinned = displayStatus.pinned,
                    onAvatarClick = { onAvatarClick(displayStatus.account.id) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Content warning
                if (hasContentWarning) {
                    ContentWarning(
                        text = displayStatus.spoilerText,
                        emojis = displayStatus.emojis,
                        expanded = showContentBehindWarning,
                        onToggle = { showContentBehindWarning = !showContentBehindWarning },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Status content
                if (showBody) {
                    StatusContent(
                        content = displayStatus.content,
                        mentions = displayStatus.mentions,
                        tags = displayStatus.tags,
                        emojis = displayStatus.emojis,
                        onMentionClick = onMentionClick,
                        onHashtagClick = onHashtagClick,
                        onLinkClick = onLinkClick,
                        onStatusClick = { onStatusClick(displayStatus.id) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Media attachments
                if (showBody && displayStatus.mediaAttachments.isNotEmpty()) {
                    MediaAttachmentGrid(
                        attachments = displayStatus.mediaAttachments,
                        sensitive = displayStatus.sensitive,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Poll
                displayStatus.poll?.takeIf { showBody }?.let { poll ->
                    PollComponent(
                        poll = poll,
                        emojis = displayStatus.emojis,
                        onVoteClick = onVoteClick,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Quoted status
                displayStatus.quote?.takeIf { showBody }?.let { quote ->
                    QuotedStatusCard(
                        quote = quote,
                        onClick = onStatusClick,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Interaction counts and buttons
                StatusActions(
                    status = displayStatus,
                    onFavoriteClick = { onFavoriteClick(displayStatus) },
                    onBoostClick = { onBoostClick(displayStatus) },
                    onReplyClick = { onReplyClick(displayStatus) },
                    onBookmarkClick = { onBookmarkClick(displayStatus) },
                    onShareClick = { onShareClick(displayStatus) },
                    onPinClick = onPinClick?.let { { it(displayStatus) } },
                    onReportClick = onReportClick?.let { { it(displayStatus) } }
                )
            }
        }
    }
}

/**
 * Displays boost indicator when a status is reblogged.
 */
@Composable
private fun BoostIndicator(
    boosterName: String,
    boosterEmojis: List<com.pfitzer.tootsie.domain.model.CustomEmoji>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        EmojiText(
            text = stringResource(R.string.status_boosted, boosterName),
            emojis = boosterEmojis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Displays account information and timestamp.
 */
@Composable
private fun StatusHeader(
    account: com.pfitzer.tootsie.domain.model.Account,
    timestamp: String,
    isPinned: Boolean = false,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = account.avatar,
            contentDescription = "${account.displayName}'s avatar",
            modifier = Modifier
                .size(48.dp)
                .clickable { onAvatarClick() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            EmojiText(
                text = account.displayName,
                emojis = account.emojis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${account.acct}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pinned indicator
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = stringResource(R.string.cd_pinned),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = LocaleAwareDateFormatter.formatRelativeTime(LocalContext.current, timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays content warning text.
 */
@Composable
internal fun ContentWarning(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<com.pfitzer.tootsie.domain.model.CustomEmoji> = emptyList()
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiText(
                text = stringResource(R.string.status_content_warning, text),
                emojis = emojis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            TextButton(onClick = onToggle) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.status_show_less else R.string.status_show_more
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Displays status content (text) with rich HTML rendering.
 */
@Composable
private fun StatusContent(
    content: String,
    mentions: List<com.pfitzer.tootsie.domain.model.Mention>,
    tags: List<com.pfitzer.tootsie.domain.model.Tag>,
    emojis: List<com.pfitzer.tootsie.domain.model.CustomEmoji>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (content.isNotBlank()) {
        RichTextContent(
            content = content,
            mentions = mentions,
            tags = tags,
            onMentionClick = onMentionClick,
            onHashtagClick = onHashtagClick,
            onLinkClick = onLinkClick,
            onTextClick = onStatusClick,
            modifier = modifier.fillMaxWidth(),
            emojis = emojis
        )
    }
}

/**
 * Displays media attachments in a grid.
 */
@Composable
private fun MediaAttachmentGrid(
    attachments: List<com.pfitzer.tootsie.domain.model.MediaAttachment>,
    sensitive: Boolean,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    // Sensitive media is blurred until the user reveals it
    var revealed by rememberSaveable(attachments.first().id) { mutableStateOf(!sensitive) }

    Box(modifier = modifier) {
        val gridModifier = if (revealed) Modifier else Modifier.blur(32.dp)
        when (attachments.size) {
            1 -> SingleMediaAttachment(attachments[0], onImageClick, onVideoClick, gridModifier)
            2 -> TwoMediaGrid(attachments, onImageClick, onVideoClick, gridModifier)
            3 -> ThreeMediaGrid(attachments, onImageClick, onVideoClick, gridModifier)
            else -> FourPlusMediaGrid(attachments, onImageClick, onVideoClick, gridModifier)
        }
        if (!revealed) {
            // Covers the grid and consumes clicks so media can't be opened while hidden
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { revealed = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.media_sensitive_tap_to_show),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Display single media attachment.
 */
@Composable
private fun SingleMediaAttachment(
    attachment: com.pfitzer.tootsie.domain.model.MediaAttachment,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .then(
                when (attachment.type) {
                    MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                        Modifier.clickable {
                            onImageClick(attachment.url, attachment.description)
                        }
                    }
                    MediaType.VIDEO -> {
                        Modifier.clickable {
                            onVideoClick(attachment.url, attachment.description)
                        }
                    }
                    else -> Modifier
                }
            )
    ) {
        when (attachment.type) {
            MediaType.IMAGE, MediaType.GIFV -> {
                AsyncImage(
                    model = attachment.previewUrl ?: attachment.url,
                    contentDescription = attachment.description ?: stringResource(R.string.cd_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            MediaType.VIDEO -> {
                AsyncImage(
                    model = attachment.previewUrl ?: attachment.url,
                    contentDescription = attachment.description ?: stringResource(R.string.cd_video_thumbnail),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_video),
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            MediaType.AUDIO -> {
                // Audio placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.media_label_audio),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MediaType.UNKNOWN -> {
                // Remote media the server has not cached is often still served via its
                // media proxy, so try to load it and fall back to a placeholder
                SubcomposeAsyncImage(
                    model = attachment.previewUrl ?: attachment.url,
                    contentDescription = attachment.description ?: stringResource(R.string.cd_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.media_preview_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Display two media attachments side by side.
 */
@Composable
private fun TwoMediaGrid(
    attachments: List<com.pfitzer.tootsie.domain.model.MediaAttachment>,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        attachments.take(2).forEach { attachment ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        when (attachment.type) {
                            MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                                Modifier.clickable {
                                    onImageClick(attachment.url, attachment.description)
                                }
                            }
                            MediaType.VIDEO -> {
                                Modifier.clickable {
                                    onVideoClick(attachment.url, attachment.description)
                                }
                            }
                            else -> Modifier
                        }
                    )
            ) {
                AsyncImage(
                    model = attachment.previewUrl ?: attachment.url,
                    contentDescription = attachment.description,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (attachment.type == MediaType.VIDEO || attachment.type == MediaType.GIFV) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Display three media attachments in a grid.
 */
@Composable
private fun ThreeMediaGrid(
    attachments: List<com.pfitzer.tootsie.domain.model.MediaAttachment>,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // First item takes half width
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    when (attachments[0].type) {
                        MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                            Modifier.clickable {
                                onImageClick(attachments[0].url, attachments[0].description)
                            }
                        }
                        MediaType.VIDEO -> {
                            Modifier.clickable {
                                onVideoClick(attachments[0].url, attachments[0].description)
                            }
                        }
                        else -> Modifier
                    }
                )
        ) {
            AsyncImage(
                model = attachments[0].previewUrl ?: attachments[0].url,
                contentDescription = attachments[0].description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Other two in a column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            attachments.drop(1).take(2).forEach { attachment ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            when (attachment.type) {
                                MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                                    Modifier.clickable {
                                        onImageClick(attachment.url, attachment.description)
                                    }
                                }
                                MediaType.VIDEO -> {
                                    Modifier.clickable {
                                        onVideoClick(attachment.url, attachment.description)
                                    }
                                }
                                else -> Modifier
                            }
                        )
                ) {
                    AsyncImage(
                        model = attachment.previewUrl ?: attachment.url,
                        contentDescription = attachment.description,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Display four or more media attachments in a 2x2 grid.
 */
@Composable
private fun FourPlusMediaGrid(
    attachments: List<com.pfitzer.tootsie.domain.model.MediaAttachment>,
    onImageClick: (String, String?) -> Unit,
    onVideoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            attachments.take(2).forEach { attachment ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            when (attachment.type) {
                                MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                                    Modifier.clickable {
                                        onImageClick(attachment.url, attachment.description)
                                    }
                                }
                                MediaType.VIDEO -> {
                                    Modifier.clickable {
                                        onVideoClick(attachment.url, attachment.description)
                                    }
                                }
                                else -> Modifier
                            }
                        )
                ) {
                    AsyncImage(
                        model = attachment.previewUrl ?: attachment.url,
                        contentDescription = attachment.description,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        // Bottom row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            attachments.drop(2).take(2).forEach { attachment ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            when (attachment.type) {
                                MediaType.IMAGE, MediaType.GIFV, MediaType.UNKNOWN -> {
                                    Modifier.clickable {
                                        onImageClick(attachment.url, attachment.description)
                                    }
                                }
                                MediaType.VIDEO -> {
                                    Modifier.clickable {
                                        onVideoClick(attachment.url, attachment.description)
                                    }
                                }
                                else -> Modifier
                            }
                        )
                ) {
                    AsyncImage(
                        model = attachment.previewUrl ?: attachment.url,
                        contentDescription = attachment.description,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Show "+N" overlay if more than 4 attachments
                    if (attachments.size > 4 && attachment == attachments[3]) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.status_more_media, attachments.size - 4),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
