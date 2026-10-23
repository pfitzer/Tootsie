package com.pfitzer.tootsie.presentation.components.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.PollConfig

/**
 * Component for creating and editing a poll in the compose screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePollComponent(
    pollConfig: PollConfig,
    onUpdateOption: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onUpdateExpiry: (Int) -> Unit,
    onToggleMultipleChoice: () -> Unit,
    onToggleHideTotals: () -> Unit,
    onRemovePoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExpiryMenu by remember { mutableStateOf(false) }

    val expiryOptions = listOf(
        300 to stringResource(R.string.poll_expiry_5_minutes),
        1800 to stringResource(R.string.poll_expiry_30_minutes),
        3600 to stringResource(R.string.poll_expiry_1_hour),
        21600 to stringResource(R.string.poll_expiry_6_hours),
        86400 to stringResource(R.string.poll_expiry_1_day),
        259200 to stringResource(R.string.poll_expiry_3_days),
        604800 to stringResource(R.string.poll_expiry_7_days)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.poll),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onRemovePoll) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_poll)
                    )
                }
            }

            // Options
            pollConfig.options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { onUpdateOption(index, it) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(stringResource(R.string.poll_option_placeholder, index + 1))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    if (pollConfig.options.size > 2) {
                        IconButton(onClick = { onRemoveOption(index) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_poll_option)
                            )
                        }
                    } else {
                        // Spacer to maintain alignment when no delete button
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Add option button
            if (pollConfig.options.size < 4) {
                TextButton(
                    onClick = onAddOption,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_poll_option))
                }
            }

            HorizontalDivider()

            // Multiple choice toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.poll_multiple_choice))
                Switch(
                    checked = pollConfig.multiple,
                    onCheckedChange = { onToggleMultipleChoice() }
                )
            }

            // Hide totals toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.poll_hide_totals))
                Switch(
                    checked = pollConfig.hideTotals,
                    onCheckedChange = { onToggleHideTotals() }
                )
            }

            // Expiry duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.poll_expiry))
                Box {
                    TextButton(onClick = { showExpiryMenu = true }) {
                        Text(expiryOptions.find { it.first == pollConfig.expiresIn }?.second ?: "")
                    }
                    DropdownMenu(
                        expanded = showExpiryMenu,
                        onDismissRequest = { showExpiryMenu = false }
                    ) {
                        expiryOptions.forEach { (seconds, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onUpdateExpiry(seconds)
                                    showExpiryMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
