package com.pfitzer.tootsie.presentation.screens.filters

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Filters Screen - displays and manages user's keyword filters.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToFilter Callback to edit a filter (filterId), or create one (null)
 * @param viewModel ViewModel for managing filters screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFilter: (String?) -> Unit = {},
    viewModel: FiltersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterToDelete by remember { mutableStateOf<Filter?>(null) }

    // Reload when coming back from the editor
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(FiltersAction.LoadFilters)
        onPauseOrDispose { }
    }

    filterToDelete?.let { filter ->
        AlertDialog(
            onDismissRequest = { filterToDelete = null },
            title = { Text(stringResource(R.string.filter_delete_confirm, filter.title)) },
            text = { Text(stringResource(R.string.filter_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(FiltersAction.DeleteFilter(filter.id))
                    filterToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { filterToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.filters_title)) },
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
        },
        floatingActionButton = {
            if (uiState.filters.isNotEmpty()) {
                FloatingActionButton(onClick = { onNavigateToFilter(null) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.filter_create)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Reloads keep the list visible
                uiState.isLoading && uiState.filters.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.filters.isEmpty() -> {
                    EmptyFilters(onCreate = { onNavigateToFilter(null) })
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = uiState.filters,
                            key = { it.id }
                        ) { filter ->
                            FilterItem(
                                filter = filter,
                                onClick = { onNavigateToFilter(filter.id) },
                                onDelete = { filterToDelete = filter }
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onAction(FiltersAction.DismissError) }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                ) {
                    Text(error.asString())
                }
            }
        }
    }
}

/**
 * A filter: title, keywords, and action/contexts/expiry as plain text.
 */
@Composable
private fun FilterItem(
    filter: Filter,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val details = buildList {
        add(stringResource(filter.filterAction.labelRes))
        add(filter.context.map { stringResource(it.labelRes) }.joinToString(", "))
        filter.expiresAt?.let { add(stringResource(R.string.filter_expires_at, formatFilterExpiry(it))) }
    }.joinToString(" · ")

    ListItem(
        headlineContent = { Text(filter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(
                    text = filter.keywords.joinToString(", ") { it.keyword },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = { Icon(Icons.Default.FilterList, contentDescription = null) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.filter_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptyFilters(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.filters_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.filters_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.filter_create))
        }
    }
}

@get:StringRes
val FilterContext.labelRes: Int
    get() = when (this) {
        FilterContext.HOME -> R.string.filter_context_home
        FilterContext.NOTIFICATIONS -> R.string.filter_context_notifications
        FilterContext.PUBLIC -> R.string.filter_context_public
        FilterContext.THREAD -> R.string.filter_context_thread
        FilterContext.ACCOUNT -> R.string.filter_context_account
    }

@get:StringRes
val FilterAction.labelRes: Int
    get() = when (this) {
        FilterAction.WARN -> R.string.filter_action_warn
        FilterAction.HIDE -> R.string.filter_action_hide
    }

/**
 * Expiry date of a filter (ISO 8601) in the device's date format.
 */
fun formatFilterExpiry(expiresAt: String?): String {
    if (expiresAt == null) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(expiresAt))
    }.getOrDefault(expiresAt)
}
