package com.pfitzer.tootsie.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pfitzer.tootsie.domain.model.Account

/**
 * Settings screen showing account management.
 * Allows users to:
 * - View all logged-in accounts
 * - Switch between accounts
 * - Remove accounts
 * - Add new accounts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle logout navigation
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.LoggedOut) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SettingsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SettingsUiState.Success -> {
                    SuccessContent(
                        state = state,
                        onAction = viewModel::onAction,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }

                is SettingsUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.onAction(SettingsAction.Refresh) }
                    )
                }

                is SettingsUiState.LoggedOut -> {
                    // Navigation handled in LaunchedEffect
                }
            }
        }
    }
}

/**
 * Success state content showing account list.
 */
@Composable
private fun SuccessContent(
    state: SettingsUiState.Success,
    onAction: (SettingsAction) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Account list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.accounts, key = { it.id }) { account ->
                AccountItem(
                    account = account,
                    isActive = account.id == state.activeAccountId,
                    onSwitch = { onAction(SettingsAction.SwitchAccount(account.id)) },
                    onRemove = { onAction(SettingsAction.RemoveAccount(account.id)) },
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }

            // Add account button
            item {
                AddAccountButton(
                    onClick = onNavigateToLogin,
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }
        }

        // Error message
        state.errorMessage?.let { errorMessage ->
            ErrorBanner(
                message = errorMessage,
                onDismiss = { onAction(SettingsAction.DismissError) }
            )
        }
    }

    // Removal confirmation dialog
    if (state.accountToRemove != null) {
        val accountToRemove = state.accounts.find { it.id == state.accountToRemove }
        RemoveAccountDialog(
            account = accountToRemove,
            onConfirm = { onAction(SettingsAction.ConfirmRemoveAccount) },
            onDismiss = { onAction(SettingsAction.CancelRemoveAccount) }
        )
    }
}

/**
 * Single account item card.
 */
@Composable
private fun AccountItem(
    account: Account,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled && !isActive) { onSwitch() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = account.avatar,
                contentDescription = "Avatar for ${account.displayName}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Account info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.titleMedium,
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

            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active account",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove account",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Add account button.
 */
@Composable
private fun AddAccountButton(
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Another Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Remove account confirmation dialog.
 */
@Composable
private fun RemoveAccountDialog(
    account: Account?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Account?") },
        text = {
            Text(
                if (account != null) {
                    "Are you sure you want to remove @${account.acct}? This will delete all cached data for this account."
                } else {
                    "Are you sure you want to remove this account?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Error content state.
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Error banner at bottom of screen.
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
