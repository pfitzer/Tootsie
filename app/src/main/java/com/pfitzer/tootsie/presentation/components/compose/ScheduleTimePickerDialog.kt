package com.pfitzer.tootsie.presentation.components.compose

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.util.ScheduleTime
import java.time.Instant
import java.time.ZoneId

/**
 * Picks the publication time of a scheduled post: first the date, then the time of day.
 *
 * @param initial Preselected time
 * @param onConfirm Called with the picked time (validated by the caller)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimePickerDialog(
    initial: Instant,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { ScheduleTime.toDatePickerMillis(Instant.now(), zone) }
    val initialTime = remember(initial) { initial.atZone(zone) }
    var pickedDate by rememberSaveable { mutableStateOf<Long?>(null) }

    val date = pickedDate
    if (date == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = ScheduleTime.toDatePickerMillis(initial, zone),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            }
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDate = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.schedule_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(ScheduleTime.fromPicker(date, timeState.hour, timeState.minute, zone)) }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }
}
