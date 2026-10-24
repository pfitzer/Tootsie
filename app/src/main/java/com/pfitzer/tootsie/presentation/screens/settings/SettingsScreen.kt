package com.pfitzer.tootsie.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pfitzer.tootsie.R
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
    onNavigateToPushSettings: () -> Unit = {},
    onNavigateToFollowRequests: () -> Unit = {},
    onNavigateToMutedAccounts: () -> Unit = {},
    onNavigateToBlockedAccounts: () -> Unit = {},
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToPushSettings = onNavigateToPushSettings,
                        onNavigateToFollowRequests = onNavigateToFollowRequests,
                        onNavigateToMutedAccounts = onNavigateToMutedAccounts,
                        onNavigateToBlockedAccounts = onNavigateToBlockedAccounts
                    )
                }

                is SettingsUiState.Error -> {
                    ErrorContent(
                        message = state.message.asString(),
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
    onNavigateToLogin: () -> Unit,
    onNavigateToPushSettings: () -> Unit,
    onNavigateToFollowRequests: () -> Unit,
    onNavigateToMutedAccounts: () -> Unit,
    onNavigateToBlockedAccounts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accounts Section
            item {
                Text(
                    text = stringResource(R.string.accounts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(state.accounts, key = { it.id }) { account ->
                AccountItem(
                    account = account,
                    isActive = account.id == state.activeAccountId,
                    onSwitch = { onAction(SettingsAction.SwitchAccount(account.id)) },
                    onRemove = { onAction(SettingsAction.RemoveAccount(account.id)) },
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }

            // Push settings button
            item {
                PushSettingsButton(
                    onClick = onNavigateToPushSettings,
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }

            // Follow requests button
            item {
                FollowRequestsButton(
                    onClick = onNavigateToFollowRequests,
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }

            // Muted accounts button
            item {
                MutedAccountsButton(
                    onClick = onNavigateToMutedAccounts,
                    isEnabled = !state.isSwitching && !state.isRemoving
                )
            }

            // Blocked accounts button
            item {
                BlockedAccountsButton(
                    onClick = onNavigateToBlockedAccounts,
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

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Theme Section
            item {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                ThemeSelectionCard(
                    currentTheme = state.theme,
                    onThemeChange = { onAction(SettingsAction.ChangeTheme(it)) }
                )
            }

            // Appearance Section
            item {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                TextSizeCard(
                    currentTextSize = state.textSize,
                    onTextSizeChange = { onAction(SettingsAction.ChangeTextSize(it)) }
                )
            }

            // Database Cleanup Section
            item {
                Text(
                    text = stringResource(R.string.database_cleanup),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                DatabaseRetentionCard(
                    currentRetentionHours = state.databaseRetentionHours,
                    onRetentionChange = { onAction(SettingsAction.ChangeDatabaseRetentionHours(it)) }
                )
            }
        }

        // Error message
        state.errorMessage?.let { errorMessage ->
            ErrorBanner(
                message = errorMessage.asString(),
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
private fun ThemeSelectionCard(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ThemeOption(
                label = stringResource(R.string.theme_light),
                selected = currentTheme == "light",
                onClick = { onThemeChange("light") }
            )
            ThemeOption(
                label = stringResource(R.string.theme_dark),
                selected = currentTheme == "dark",
                onClick = { onThemeChange("dark") }
            )
            ThemeOption(
                label = stringResource(R.string.theme_system),
                selected = currentTheme == "system",
                onClick = { onThemeChange("system") }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Card for selecting text size.
 */
@Composable
private fun TextSizeCard(
    currentTextSize: String,
    onTextSizeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.text_size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ThemeOption(
                label = stringResource(R.string.text_size_small),
                selected = currentTextSize == "SMALL",
                onClick = { onTextSizeChange("SMALL") }
            )
            ThemeOption(
                label = stringResource(R.string.text_size_medium),
                selected = currentTextSize == "MEDIUM",
                onClick = { onTextSizeChange("MEDIUM") }
            )
            ThemeOption(
                label = stringResource(R.string.text_size_large),
                selected = currentTextSize == "LARGE",
                onClick = { onTextSizeChange("LARGE") }
            )
            ThemeOption(
                label = stringResource(R.string.text_size_extra_large),
                selected = currentTextSize == "EXTRA_LARGE",
                onClick = { onTextSizeChange("EXTRA_LARGE") }
            )
        }
    }
}

/**
 * Card for selecting database retention period.
 */
@Composable
private fun DatabaseRetentionCard(
    currentRetentionHours: String,
    onRetentionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.retention_period),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ThemeOption(
                label = stringResource(R.string.retention_12h),
                selected = currentRetentionHours == "12",
                onClick = { onRetentionChange("12") }
            )
            ThemeOption(
                label = stringResource(R.string.retention_24h),
                selected = currentRetentionHours == "24",
                onClick = { onRetentionChange("24") }
            )
            ThemeOption(
                label = stringResource(R.string.retention_48h),
                selected = currentRetentionHours == "48",
                onClick = { onRetentionChange("48") }
            )
        }
    }
}

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
                    contentDescription = stringResource(R.string.settings_active_account),
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
                    contentDescription = stringResource(R.string.settings_remove_account),
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
                text = stringResource(R.string.settings_add_account),
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
        title = { Text(stringResource(R.string.settings_remove_dialog_title)) },
        text = {
            Text(
                if (account != null) {
                    stringResource(R.string.settings_remove_dialog_message, account.acct)
                } else {
                    stringResource(R.string.settings_remove_dialog_message_generic)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
            Text(stringResource(R.string.retry))
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
                Text(stringResource(R.string.dismiss))
            }
        }
    }
}

/**
 * Push settings button.
 */
@Composable
private fun PushSettingsButton(
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.push_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Follow requests button.
 */
@Composable
private fun FollowRequestsButton(
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.follow_requests_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Muted accounts button.
 */
@Composable
private fun MutedAccountsButton(
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.muted_accounts_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Blocked accounts button.
 */
@Composable
private fun BlockedAccountsButton(
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.blocked_accounts_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
