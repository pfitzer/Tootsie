package com.pfitzer.tootsie.presentation.screens.settings.push

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.R

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
                                viewModel.onAction(PushSettingsAction.TogglePush(enabled))
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
