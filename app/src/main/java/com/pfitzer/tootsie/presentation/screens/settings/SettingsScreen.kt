package com.pfitzer.tootsie.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pfitzer.tootsie.BuildConfig
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account

private const val SOURCE_CODE_URL = "https://github.com/pfitzer/Tootsie"

/**
 * Settings screen.
 * Sections:
 * - Accounts: switch, remove and add accounts
 * - Active account: profile, push, follow requests, filters, muted and blocked accounts
 * - Appearance: theme and text size
 * - Storage: how long cached timelines are kept
 * - About: version and source code
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
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFilters: () -> Unit = {},
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
                        navigation = SettingsNavigation(
                            onAddAccount = onNavigateToLogin,
                            onEditProfile = onNavigateToEditProfile,
                            onPushSettings = onNavigateToPushSettings,
                            onFollowRequests = onNavigateToFollowRequests,
                            onFilters = onNavigateToFilters,
                            onMutedAccounts = onNavigateToMutedAccounts,
                            onBlockedAccounts = onNavigateToBlockedAccounts
                        )
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
 * Navigation targets reachable from the settings screen.
 */
private class SettingsNavigation(
    val onAddAccount: () -> Unit,
    val onEditProfile: () -> Unit,
    val onPushSettings: () -> Unit,
    val onFollowRequests: () -> Unit,
    val onFilters: () -> Unit,
    val onMutedAccounts: () -> Unit,
    val onBlockedAccounts: () -> Unit
)

/**
 * Success state content.
 */
@Composable
private fun SuccessContent(
    state: SettingsUiState.Success,
    onAction: (SettingsAction) -> Unit,
    navigation: SettingsNavigation
) {
    val isEnabled = !state.isSwitching && !state.isRemoving
    val activeAccount = state.accounts.find { it.id == state.activeAccountId }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Accounts
            item { SectionHeader(stringResource(R.string.accounts)) }

            items(state.accounts, key = { it.id }) { account ->
                AccountItem(
                    account = account,
                    isActive = account.id == state.activeAccountId,
                    onSwitch = { onAction(SettingsAction.SwitchAccount(account.id)) },
                    onRemove = { onAction(SettingsAction.RemoveAccount(account.id)) },
                    isEnabled = isEnabled
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_add_account),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    },
                    modifier = Modifier.clickable(enabled = isEnabled, onClick = navigation.onAddAccount)
                )
            }

            // Settings of the active account
            item {
                SectionHeader(
                    if (activeAccount != null) {
                        stringResource(R.string.settings_section_active_account, activeAccount.acct)
                    } else {
                        stringResource(R.string.settings_active_account)
                    }
                )
            }

            item {
                NavigationItem(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.edit_profile),
                    description = stringResource(R.string.settings_edit_profile_description),
                    enabled = isEnabled,
                    onClick = navigation.onEditProfile
                )
            }
            item {
                NavigationItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.push_notifications),
                    description = stringResource(R.string.settings_push_description),
                    enabled = isEnabled,
                    onClick = navigation.onPushSettings
                )
            }
            item {
                NavigationItem(
                    icon = Icons.Default.PersonAdd,
                    title = stringResource(R.string.follow_requests_title),
                    description = stringResource(R.string.settings_follow_requests_description),
                    enabled = isEnabled,
                    onClick = navigation.onFollowRequests
                )
            }
            item {
                NavigationItem(
                    icon = Icons.Default.FilterList,
                    title = stringResource(R.string.filters_title),
                    description = stringResource(R.string.settings_filters_description),
                    enabled = isEnabled,
                    onClick = navigation.onFilters
                )
            }
            item {
                NavigationItem(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    title = stringResource(R.string.muted_accounts_title),
                    description = stringResource(R.string.settings_muted_description),
                    enabled = isEnabled,
                    onClick = navigation.onMutedAccounts
                )
            }
            item {
                NavigationItem(
                    icon = Icons.Default.Block,
                    title = stringResource(R.string.blocked_accounts_title),
                    description = stringResource(R.string.settings_blocked_description),
                    enabled = isEnabled,
                    onClick = navigation.onBlockedAccounts
                )
            }

            // Appearance
            item { SectionHeader(stringResource(R.string.appearance)) }

            item {
                SegmentedSetting(
                    title = stringResource(R.string.theme),
                    options = listOf(
                        "light" to stringResource(R.string.theme_light),
                        "dark" to stringResource(R.string.theme_dark),
                        "system" to stringResource(R.string.theme_system_short)
                    ),
                    selected = state.theme,
                    onSelect = { onAction(SettingsAction.ChangeTheme(it)) }
                )
            }

            item {
                TextSizeItem(
                    currentTextSize = state.textSize,
                    onTextSizeChange = { onAction(SettingsAction.ChangeTextSize(it)) }
                )
            }

            // Storage
            item { SectionHeader(stringResource(R.string.settings_section_storage)) }

            item {
                SegmentedSetting(
                    title = stringResource(R.string.settings_cache_retention),
                    description = stringResource(R.string.settings_cache_retention_description),
                    options = listOf(
                        "12" to stringResource(R.string.retention_12h),
                        "24" to stringResource(R.string.retention_24h),
                        "48" to stringResource(R.string.retention_48h)
                    ),
                    selected = state.databaseRetentionHours,
                    onSelect = { onAction(SettingsAction.ChangeDatabaseRetentionHours(it)) }
                )
            }

            // About
            item { SectionHeader(stringResource(R.string.settings_section_about)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }

            item {
                val uriHandler = LocalUriHandler.current
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_source_code)) },
                    supportingContent = { Text(SOURCE_CODE_URL.removePrefix("https://")) },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.clickable { uriHandler.openUri(SOURCE_CODE_URL) }
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
 * Section header in the settings list.
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * List entry that opens another screen.
 */
@Composable
private fun NavigationItem(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

/**
 * Setting with a few options, shown as a segmented button row.
 */
@Composable
private fun SegmentedSetting(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    description: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

/**
 * Text size entry showing the current size; opens a dialog to change it.
 */
@Composable
private fun TextSizeItem(
    currentTextSize: String,
    onTextSizeChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        "SMALL" to stringResource(R.string.text_size_small),
        "MEDIUM" to stringResource(R.string.text_size_medium),
        "LARGE" to stringResource(R.string.text_size_large),
        "EXTRA_LARGE" to stringResource(R.string.text_size_extra_large)
    )
    val currentLabel = options.firstOrNull { it.first == currentTextSize }?.second.orEmpty()

    ListItem(
        headlineContent = { Text(stringResource(R.string.text_size)) },
        supportingContent = { Text(currentLabel) },
        leadingContent = { Icon(Icons.Default.FormatSize, contentDescription = null) },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.text_size)) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = value == currentTextSize,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onTextSizeChange(value)
                                        showDialog = false
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == currentTextSize,
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Account entry. Tapping switches to the account; removing is in the overflow menu.
 */
@Composable
private fun AccountItem(
    account: Account,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit,
    isEnabled: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = account.displayName.ifBlank { account.username },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "@${account.acct}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AsyncImage(
                model = account.avatar,
                contentDescription = stringResource(R.string.cd_avatar),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Text(
                        text = stringResource(R.string.settings_account_active_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        enabled = isEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.settings_account_options)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.settings_remove_account),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        },
        colors = if (isActive) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        } else {
            ListItemDefaults.colors()
        },
        modifier = Modifier.clickable(enabled = isEnabled && !isActive, onClick = onSwitch)
    )
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
