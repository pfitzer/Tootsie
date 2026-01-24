package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.common.AvatarImage
import com.pfitzer.tootsie.presentation.util.formatRelativeTime
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

    // Check if status is filtered with WARN action
    val isFiltered = status.filteredByKeyword != null && !showFilteredContent

    if (isFiltered) {
        // Show placeholder for filtered content
        FilteredContentPlaceholder(
            filterTitle = status.filteredByKeyword!!,
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Account header
                StatusHeader(
                    account = displayStatus.account,
                    timestamp = displayStatus.createdAt,
                    onAvatarClick = { onAvatarClick(displayStatus.account.id) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Content warning
                if (displayStatus.spoilerText.isNotBlank()) {
                    ContentWarning(
                        text = displayStatus.spoilerText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Status content
                StatusContent(
                    content = displayStatus.content,
                    mentions = displayStatus.mentions,
                    tags = displayStatus.tags,
                    sensitive = displayStatus.sensitive,
                    spoilerText = displayStatus.spoilerText,
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick,
                    onLinkClick = onLinkClick,
                    onStatusClick = { onStatusClick(displayStatus.id) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Media attachments
                if (displayStatus.mediaAttachments.isNotEmpty()) {
                    MediaAttachmentGrid(
                        attachments = displayStatus.mediaAttachments,
                        sensitive = displayStatus.sensitive,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Poll
                displayStatus.poll?.let { poll ->
                    PollComponent(
                        poll = poll,
                        onVoteClick = onVoteClick,
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
                    onShareClick = { onShareClick(displayStatus) }
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
        Text(
            text = stringResource(R.string.status_boosted, boosterName),
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
            Text(
                text = account.displayName,
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

        Text(
            text = formatRelativeTime(timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays content warning text.
 */
@Composable
private fun ContentWarning(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = stringResource(R.string.status_content_warning, text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(8.dp)
        )
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
    sensitive: Boolean,
    spoilerText: String,
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
            modifier = modifier.fillMaxWidth()
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

    when (attachments.size) {
        1 -> SingleMediaAttachment(attachments[0], onImageClick, onVideoClick, modifier)
        2 -> TwoMediaGrid(attachments, onImageClick, onVideoClick, modifier)
        3 -> ThreeMediaGrid(attachments, onImageClick, onVideoClick, modifier)
        else -> FourPlusMediaGrid(attachments, onImageClick, onVideoClick, modifier)
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
                    MediaType.IMAGE, MediaType.GIFV -> {
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.media_label_media),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                            MediaType.IMAGE, MediaType.GIFV -> {
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
                        MediaType.IMAGE, MediaType.GIFV -> {
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
                                MediaType.IMAGE, MediaType.GIFV -> {
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
                                MediaType.IMAGE, MediaType.GIFV -> {
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
                                MediaType.IMAGE, MediaType.GIFV -> {
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
