package com.pfitzer.tootsie.presentation.screens.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext

/**
 * Filters Screen - displays and manages user's keyword filters.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToFilter Callback to navigate to filter detail (filterId)
 * @param viewModel ViewModel for managing filters screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFilter: (String) -> Unit = {},
    viewModel: FiltersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    // Create filter dialog
    if (showCreateDialog) {
        CreateFilterDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, contexts, action, expiresIn, keywords ->
                viewModel.onAction(FiltersAction.CreateFilter(title, contexts, action, expiresIn, keywords))
                showCreateDialog = false
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
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.filter_create)
                )
            }
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
                uiState.filters.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.filters_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.filters_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.filters,
                            key = { it.id }
                        ) { filter ->
                            FilterItem(
                                filter = filter,
                                onClick = { onNavigateToFilter(filter.id) },
                                onDelete = {
                                    viewModel.onAction(FiltersAction.DeleteFilter(filter.id))
                                }
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
 * Filter item card.
 */
@Composable
private fun FilterItem(
    filter: Filter,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = filter.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_filter),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Context badges
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filter.context) { context ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when (context) {
                                    FilterContext.HOME -> stringResource(R.string.filter_context_home)
                                    FilterContext.NOTIFICATIONS -> stringResource(R.string.filter_context_notifications)
                                    FilterContext.PUBLIC -> stringResource(R.string.filter_context_public)
                                    FilterContext.THREAD -> stringResource(R.string.filter_context_thread)
                                    FilterContext.ACCOUNT -> stringResource(R.string.filter_context_account)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keywords summary
            Text(
                text = filter.keywords.joinToString(", ") { it.keyword },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action badge
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = when (filter.filterAction) {
                            FilterAction.WARN -> stringResource(R.string.filter_action_warn)
                            FilterAction.HIDE -> stringResource(R.string.filter_action_hide)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

/**
 * Dialog for creating a new filter.
 */
@Composable
private fun CreateFilterDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        contexts: List<FilterContext>,
        action: FilterAction,
        expiresIn: Int?,
        keywords: List<Pair<String, Boolean>>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedContexts by remember { mutableStateOf(setOf(FilterContext.HOME)) }
    var selectedAction by remember { mutableStateOf(FilterAction.WARN) }
    var keywords by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var newKeyword by remember { mutableStateOf("") }
    var wholeWord by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_create)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.filter_title_label)) },
                    placeholder = { Text(stringResource(R.string.filter_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contexts
                Text(
                    text = stringResource(R.string.filter_contexts),
                    style = MaterialTheme.typography.labelMedium
                )
                Column {
                    FilterContext.entries.forEach { context ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedContexts = if (context in selectedContexts) {
                                        selectedContexts - context
                                    } else {
                                        selectedContexts + context
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = context in selectedContexts,
                                onCheckedChange = {
                                    selectedContexts = if (it) {
                                        selectedContexts + context
                                    } else {
                                        selectedContexts - context
                                    }
                                }
                            )
                            Text(
                                text = when (context) {
                                    FilterContext.HOME -> stringResource(R.string.filter_context_home)
                                    FilterContext.NOTIFICATIONS -> stringResource(R.string.filter_context_notifications)
                                    FilterContext.PUBLIC -> stringResource(R.string.filter_context_public)
                                    FilterContext.THREAD -> stringResource(R.string.filter_context_thread)
                                    FilterContext.ACCOUNT -> stringResource(R.string.filter_context_account)
                                }
                            )
                        }
                    }
                }

                // Action
                Text(
                    text = stringResource(R.string.filter_action_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAction = FilterAction.WARN }
                    ) {
                        RadioButton(
                            selected = selectedAction == FilterAction.WARN,
                            onClick = { selectedAction = FilterAction.WARN }
                        )
                        Text(stringResource(R.string.filter_action_warn))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAction = FilterAction.HIDE }
                    ) {
                        RadioButton(
                            selected = selectedAction == FilterAction.HIDE,
                            onClick = { selectedAction = FilterAction.HIDE }
                        )
                        Text(stringResource(R.string.filter_action_hide))
                    }
                }

                // Keywords
                Text(
                    text = stringResource(R.string.filter_keywords),
                    style = MaterialTheme.typography.labelMedium
                )

                // Add keyword field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        placeholder = { Text(stringResource(R.string.filter_keyword_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newKeyword.isNotBlank()) {
                                keywords = keywords + (newKeyword.trim() to wholeWord)
                                newKeyword = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.filter_keyword_add))
                    }
                }

                // Whole word checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { wholeWord = !wholeWord }
                ) {
                    Checkbox(
                        checked = wholeWord,
                        onCheckedChange = { wholeWord = it }
                    )
                    Text(stringResource(R.string.filter_keyword_whole_word))
                }

                // Keywords list
                if (keywords.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(keywords) { (keyword, isWholeWord) ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(keyword) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { keywords = keywords.filter { it.first != keyword } },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        title,
                        selectedContexts.toList(),
                        selectedAction,
                        null, // No expiration for now
                        keywords
                    )
                },
                enabled = title.isNotBlank() && selectedContexts.isNotEmpty() && keywords.isNotEmpty()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
