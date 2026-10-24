package com.pfitzer.tootsie.presentation.components.status

import com.pfitzer.tootsie.presentation.components.common.EmojiText
import com.pfitzer.tootsie.domain.model.CustomEmoji
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Poll
import com.pfitzer.tootsie.domain.model.PollOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays a poll from a status.
 *
 * @param poll The poll to display
 * @param onVoteClick Callback when a vote is cast
 * @param modifier Optional modifier
 */
@Composable
fun PollComponent(
    poll: Poll,
    onVoteClick: (String, List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<CustomEmoji> = emptyList()
) {
    // Track if user has voted locally (optimistic UI update)
    var hasVotedLocally by remember(poll.id) { mutableStateOf(false) }
    val canVote = !poll.expired && !poll.voted && !hasVotedLocally
    var selectedOptions by remember(poll.id) { mutableStateOf(emptySet<Int>()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        poll.options.forEachIndexed { index, option ->
            PollOptionItem(
                option = option,
                emojis = emojis,
                index = index,
                totalVotes = poll.votesCount,
                showResults = poll.voted || poll.expired || hasVotedLocally,
                isVoted = poll.ownVotes.contains(index) || (hasVotedLocally && selectedOptions.contains(index)),
                isSelected = selectedOptions.contains(index),
                canVote = canVote,
                onOptionClick = {
                    if (canVote) {
                        selectedOptions = if (poll.multiple) {
                            if (selectedOptions.contains(index)) {
                                selectedOptions - index
                            } else {
                                selectedOptions + index
                            }
                        } else {
                            setOf(index)
                        }
                    }
                }
            )
        }

        if (canVote && selectedOptions.isNotEmpty()) {
            Button(
                onClick = {
                    hasVotedLocally = true
                    onVoteClick(poll.id, selectedOptions.toList())
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.poll_vote))
            }
        }

        // Poll metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.poll_total_votes, poll.votesCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val footerText = when {
                poll.expired -> stringResource(R.string.poll_expired)
                poll.expiresAt != null -> {
                    val expiryDate = remember(poll.expiresAt) {
                        try {
                            ZonedDateTime.parse(poll.expiresAt)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (expiryDate != null) {
                        stringResource(
                            R.string.poll_expires_at,
                            expiryDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
                        )
                    } else {
                        ""
                    }
                }
                else -> ""
            }

            if (footerText.isNotEmpty()) {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PollOptionItem(
    option: PollOption,
    emojis: List<CustomEmoji>,
    index: Int,
    totalVotes: Int,
    showResults: Boolean,
    isVoted: Boolean,
    isSelected: Boolean,
    canVote: Boolean,
    onOptionClick: () -> Unit
) {
    val percentage = if (totalVotes > 0) {
        (option.votesCount ?: 0).toFloat() / totalVotes
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (canVote) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onOptionClick() }
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                }
            )
    ) {
        // Progress bar for results
        if (showResults) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .background(
                        if (isVoted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
            )
        } else if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isVoted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.poll_voted_hint),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                EmojiText(
                    text = option.title,
                    emojis = emojis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            if (showResults) {
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
