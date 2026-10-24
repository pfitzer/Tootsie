package com.pfitzer.tootsie.presentation.screens.filters.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.presentation.screens.filters.formatFilterExpiry
import com.pfitzer.tootsie.presentation.screens.filters.labelRes

/**
 * Create or edit a keyword filter. Nothing is sent to the server before "Save".
 *
 * @param onNavigateBack Callback to navigate back (also after saving or deleting)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilterEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onNavigateBack()
    }

    val errorText = uiState.errorMessage?.asString()
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(errorText)
            viewModel.onAction(FilterEditAction.DismissError)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.filter_delete_confirm, uiState.title)) },
            text = { Text(stringResource(R.string.filter_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.onAction(FilterEditAction.Delete)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.filter_create else R.string.filter_edit)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }, enabled = !uiState.isSaving) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_filter)
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.onAction(FilterEditAction.Save) },
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onAction(FilterEditAction.UpdateTitle(it)) },
                label = { Text(stringResource(R.string.filter_title_label)) },
                placeholder = { Text(stringResource(R.string.filter_title_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // Keywords
            SectionTitle(R.string.filter_keywords)
            OutlinedTextField(
                value = uiState.keywordInput,
                onValueChange = { viewModel.onAction(FilterEditAction.UpdateKeywordInput(it)) },
                placeholder = { Text(stringResource(R.string.filter_keyword_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.onAction(FilterEditAction.AddKeyword) }),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.onAction(FilterEditAction.AddKeyword) },
                        enabled = uiState.keywordInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.filter_keyword_add))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            uiState.keywords.forEachIndexed { index, keyword ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = keyword.keyword,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = keyword.wholeWord,
                        onClick = { viewModel.onAction(FilterEditAction.ToggleWholeWord(index)) },
                        label = { Text(stringResource(R.string.filter_keyword_whole_word)) }
                    )
                    IconButton(onClick = { viewModel.onAction(FilterEditAction.RemoveKeyword(index)) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.filter_keyword_remove, keyword.keyword)
                        )
                    }
                }
            }

            // Contexts
            SectionTitle(R.string.filter_contexts)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterContext.entries.forEach { context ->
                    FilterChip(
                        selected = context in uiState.contexts,
                        onClick = { viewModel.onAction(FilterEditAction.ToggleContext(context)) },
                        label = { Text(stringResource(context.labelRes)) }
                    )
                }
            }

            // Action
            SectionTitle(R.string.filter_action_label)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val actions = listOf(FilterAction.WARN, FilterAction.HIDE)
                actions.forEachIndexed { index, action ->
                    SegmentedButton(
                        selected = uiState.filterAction == action,
                        onClick = { viewModel.onAction(FilterEditAction.SelectAction(action)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = actions.size),
                        label = { Text(stringResource(action.labelRes)) }
                    )
                }
            }
            Text(
                text = stringResource(
                    if (uiState.filterAction == FilterAction.WARN) R.string.filter_action_warn_description
                    else R.string.filter_action_hide_description
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expiry
            SectionTitle(R.string.filter_expiration)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.expiryOptions.forEach { expiry ->
                    FilterChip(
                        selected = uiState.expiry == expiry,
                        onClick = { viewModel.onAction(FilterEditAction.SelectExpiry(expiry)) },
                        label = {
                            Text(
                                if (expiry == FilterExpiry.KEEP) {
                                    stringResource(expiry.labelRes, formatFilterExpiry(uiState.currentExpiresAt))
                                } else {
                                    stringResource(expiry.labelRes)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp)
    )
}
