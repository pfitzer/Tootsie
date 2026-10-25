package com.pfitzer.tootsie.presentation.screens.blockeddomains

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DomainDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.util.DomainName
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage

/**
 * Blocked Domains Screen - displays the domains (instances) the user has blocked.
 *
 * @param onNavigateBack Callback to navigate back
 * @param viewModel ViewModel for managing blocked domains screen state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedDomainsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockedDomainsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blockedDomainsPagingItems = viewModel.blockedDomainsPagingData.collectAsLazyPagingItems()

    // Reload after blocking a domain, so it shows up in the list
    LaunchedEffect(uiState.reload) {
        if (uiState.reload) {
            blockedDomainsPagingItems.refresh()
            viewModel.onAction(BlockedDomainsAction.ReloadConsumed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blocked_domains_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(BlockedDomainsAction.ShowBlockDialog) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.block_domain)
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
                blockedDomainsPagingItems.loadState.refresh is LoadState.Loading &&
                        blockedDomainsPagingItems.itemCount == 0 -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                blockedDomainsPagingItems.itemCount == 0 &&
                        blockedDomainsPagingItems.loadState.refresh is LoadState.NotLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DomainDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_blocked_domains),
                            style = MaterialTheme.typography.bodyLarge,
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
                            count = blockedDomainsPagingItems.itemCount,
                            key = blockedDomainsPagingItems.itemKey { it }
                        ) { index ->
                            blockedDomainsPagingItems[index]?.let { domain ->
                                BlockedDomainItem(
                                    domain = domain,
                                    isProcessing = domain in uiState.processingDomains,
                                    onUnblockClick = {
                                        viewModel.onAction(BlockedDomainsAction.UnblockDomain(domain))
                                    }
                                )
                            }
                        }

                        // Loading more indicator
                        if (blockedDomainsPagingItems.loadState.append is LoadState.Loading) {
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
        }
    }

    if (uiState.showBlockDialog) {
        BlockDomainDialog(
            onConfirm = { viewModel.onAction(BlockedDomainsAction.BlockDomain(it)) },
            onDismiss = { viewModel.onAction(BlockedDomainsAction.DismissBlockDialog) }
        )
    }
}

/**
 * Blocked domain with an unblock button.
 */
@Composable
private fun BlockedDomainItem(
    domain: String,
    isProcessing: Boolean,
    onUnblockClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = domain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.DomainDisabled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            OutlinedButton(
                onClick = onUnblockClick,
                enabled = !isProcessing
            ) {
                Text(
                    text = if (isProcessing) "..." else stringResource(R.string.unblock)
                )
            }
        }
    )
}

/**
 * Dialog to enter a domain to block; also accepts a URL or a handle of the server.
 */
@Composable
private fun BlockDomainDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val domain = DomainName.normalize(input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.block_domain)) },
        text = {
            Column {
                Text(stringResource(R.string.block_domain_confirm_message))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.domain_name)) },
                    placeholder = { Text("example.social") },
                    singleLine = true,
                    isError = input.isNotBlank() && domain == null,
                    supportingText = if (input.isNotBlank() && domain == null) {
                        { Text(stringResource(R.string.error_invalid_domain)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(input) },
                enabled = domain != null
            ) {
                Text(stringResource(R.string.block))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
