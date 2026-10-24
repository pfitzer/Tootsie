package com.pfitzer.tootsie.presentation.screens.settings.push

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R
import kotlinx.coroutines.launch

/**
 * Push notification settings screen.
 * Allows users to configure push notifications and alert preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PushSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Android 13+: notifications need a runtime permission; re-checked when returning to the app
    var notificationsAllowed by remember { mutableStateOf(areNotificationsAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = areNotificationsAllowed(context)
        onPauseOrDispose { }
    }
    val permissionDeniedMessage = stringResource(R.string.push_permission_denied)
    val openSettingsLabel = stringResource(R.string.push_open_notification_settings)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = areNotificationsAllowed(context)
        if (granted) {
            viewModel.onAction(PushSettingsAction.TogglePush(true))
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = permissionDeniedMessage,
                    actionLabel = openSettingsLabel
                )
                if (result == SnackbarResult.ActionPerformed) openNotificationSettings(context)
            }
        }
    }

    // Show error message in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.onAction(PushSettingsAction.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.push_notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card about UnifiedPush (shown when no distributor or not enabled)
            if (uiState.distributorName == "None" || !uiState.enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.push_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.push_info_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.push_fallback_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Push is on, but Android won't show the notifications
            if (uiState.enabled && !notificationsAllowed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.push_notifications_blocked_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.push_notifications_blocked_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { openNotificationSettings(context) }) {
                            Text(openSettingsLabel)
                        }
                    }
                }
            }

            // Enable/Disable Push Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_push_notifications),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.push_status,
                                    uiState.status.name
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.enabled,
                            onCheckedChange = { enabled ->
                                if (enabled && needsNotificationPermission(context)) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onAction(PushSettingsAction.TogglePush(enabled))
                                }
                            },
                            enabled = !uiState.isLoading
                        )
                    }

                    // Distributor info
                    if (uiState.enabled) {
                        Text(
                            text = stringResource(
                                R.string.push_distributor,
                                uiState.distributorName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Alert Settings Card
            if (uiState.enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.alert_types),
                            style = MaterialTheme.typography.titleMedium
                        )

                        AlertSwitch(
                            label = stringResource(R.string.mentions),
                            checked = uiState.alerts.mention,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(mention = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.follows),
                            checked = uiState.alerts.follow,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(follow = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.favorites),
                            checked = uiState.alerts.favourite,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(favourite = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.boosts),
                            checked = uiState.alerts.reblog,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(reblog = checked)
                                    )
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = stringResource(R.string.additional_alerts),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AlertSwitch(
                            label = stringResource(R.string.polls),
                            checked = uiState.alerts.poll,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(poll = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.status_posts),
                            checked = uiState.alerts.status,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(status = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.updates),
                            checked = uiState.alerts.update,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(update = checked)
                                    )
                                )
                            }
                        )

                        AlertSwitch(
                            label = stringResource(R.string.follow_requests),
                            checked = uiState.alerts.followRequest,
                            onCheckedChange = { checked ->
                                viewModel.onAction(
                                    PushSettingsAction.UpdateAlerts(
                                        uiState.alerts.copy(followRequest = checked)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Reusable alert switch component.
 */
@Composable
private fun AlertSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Android 13+ requires the POST_NOTIFICATIONS runtime permission.
 */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/**
 * Covers both the runtime permission and notifications disabled in the system settings.
 */
private fun areNotificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
