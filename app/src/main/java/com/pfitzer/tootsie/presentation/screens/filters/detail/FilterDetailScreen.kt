package com.pfitzer.tootsie.presentation.screens.filters.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.filter?.title ?: stringResource(R.string.filter_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(FilterDetailAction.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
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
                uiState.isLoading && uiState.filter == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.filter != null -> {
                    FilterDetailContent(
                        filter = uiState.filter!!,
                        isUpdating = uiState.isUpdating,
                        onAction = viewModel::onAction
                    )
                }
                uiState.errorMessage != null && uiState.filter == null -> {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.isUpdating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onAction(FilterDetailAction.DismissError) }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterDetailContent(
    filter: com.pfitzer.tootsie.domain.model.Filter,
    isUpdating: Boolean,
    onAction: (FilterDetailAction) -> Unit
) {
    var showTitleDialog by remember { mutableStateOf(false) }
    var showKeywordDialog by remember { mutableStateOf(false) }

    if (showTitleDialog) {
        var title by remember { mutableStateOf(filter.title) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text(stringResource(R.string.filter_title_label)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAction(FilterDetailAction.UpdateTitle(title))
                    showTitleDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showKeywordDialog) {
        var keyword by remember { mutableStateOf("") }
        var wholeWord by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showKeywordDialog = false },
            title = { Text(stringResource(R.string.filter_keyword_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = { Text(stringResource(R.string.filter_keyword_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { wholeWord = !wholeWord }
                    ) {
                        Checkbox(checked = wholeWord, onCheckedChange = { wholeWord = it })
                        Text(stringResource(R.string.filter_keyword_whole_word))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAction(FilterDetailAction.AddKeyword(keyword, wholeWord))
                    showKeywordDialog = false
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeywordDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column(modifier = Modifier.clickable { showTitleDialog = true }) {
                Text(
                    text = stringResource(R.string.filter_title_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = filter.title, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Contexts
        item {
            Text(
                text = stringResource(R.string.filter_contexts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterContext.entries.forEach { context ->
                    val selected = context in filter.context
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val newContexts = if (selected) {
                                filter.context - context
                            } else {
                                filter.context + context
                            }
                            if (newContexts.isNotEmpty()) {
                                onAction(FilterDetailAction.UpdateContexts(newContexts))
                            }
                        },
                        label = {
                            Text(
                                when (context) {
                                    FilterContext.HOME -> stringResource(R.string.filter_context_home)
                                    FilterContext.NOTIFICATIONS -> stringResource(R.string.filter_context_notifications)
                                    FilterContext.PUBLIC -> stringResource(R.string.filter_context_public)
                                    FilterContext.THREAD -> stringResource(R.string.filter_context_thread)
                                    FilterContext.ACCOUNT -> stringResource(R.string.filter_context_account)
                                }
                            )
                        }
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        // Action
        item {
            Text(
                text = stringResource(R.string.filter_action_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterAction.entries.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAction(FilterDetailAction.UpdateFilterAction(action)) }
                    ) {
                        RadioButton(
                            selected = filter.filterAction == action,
                            onClick = { onAction(FilterDetailAction.UpdateFilterAction(action)) }
                        )
                        Text(
                            when (action) {
                                FilterAction.WARN -> stringResource(R.string.filter_action_warn)
                                FilterAction.HIDE -> stringResource(R.string.filter_action_hide)
                            }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        // Keywords
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_keywords),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { showKeywordDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.filter_keyword_add))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filter.keywords.forEach { keyword ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = keyword.keyword, style = MaterialTheme.typography.bodyMedium)
                            if (keyword.wholeWord) {
                                Text(
                                    text = stringResource(R.string.filter_keyword_whole_word),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onAction(FilterDetailAction.DeleteKeyword(keyword.id)) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
