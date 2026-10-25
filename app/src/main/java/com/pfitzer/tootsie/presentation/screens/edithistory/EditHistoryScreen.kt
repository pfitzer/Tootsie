package com.pfitzer.tootsie.presentation.screens.edithistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.StatusEditVersion
import com.pfitzer.tootsie.presentation.components.common.EmojiText
import com.pfitzer.tootsie.presentation.util.LocaleAwareDateFormatter
import com.pfitzer.tootsie.presentation.util.htmlToPlainText

/**
 * Edit history of a status: all versions, newest first.
 *
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_history_title)) },
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
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.errorMessage!!.asString())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onAction(EditHistoryAction.Retry) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(uiState.versions) { index, version ->
                        EditVersionItem(
                            version = version,
                            isOriginal = index == uiState.versions.lastIndex,
                            isCurrent = index == 0
                        )
                    }
                }
            }
        }
    }
}

/**
 * One version: when it was written, content warning, text, poll options and attachments.
 */
@Composable
private fun EditVersionItem(
    version: StatusEditVersion,
    isOriginal: Boolean,
    isCurrent: Boolean
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LocaleAwareDateFormatter.formatAbsoluteTime(context, version.createdAt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                val label = when {
                    isCurrent -> R.string.edit_history_current
                    isOriginal -> R.string.edit_history_original
                    else -> null
                }
                label?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (version.spoilerText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                EmojiText(
                    text = version.spoilerText,
                    emojis = version.emojis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            EmojiText(
                text = htmlToPlainText(version.content),
                emojis = version.emojis,
                style = MaterialTheme.typography.bodyMedium
            )

            version.pollOptions.forEach { option ->
                Text(
                    text = "• $option",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (version.mediaAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.scheduled_media_count,
                            version.mediaAttachments.size,
                            version.mediaAttachments.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
