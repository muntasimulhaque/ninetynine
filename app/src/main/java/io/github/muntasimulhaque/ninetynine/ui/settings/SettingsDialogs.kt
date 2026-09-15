package io.github.muntasimulhaque.ninetynine.ui.settings

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.ninetynine.R

/*
 * The Settings page's three confirmations, each owning the dialog it shows:
 * the reminder's time picker, the reset of all progress, and the way to
 * system settings when the reminder's permission has been blocked. The
 * screen keeps only the state that says which one is up.
 */

/**
 * The reminder's time picker, Material 3 and themed with the app, not the
 * legacy dialog. The initial time is coerced: TimePicker throws on
 * out-of-range hours from a restored backup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = hour.coerceIn(0, 23),
        initialMinute = minute.coerceIn(0, 59),
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.reminder_time)) },
        text = {
            TimePicker(state = timeState)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** The one destructive act in the app: wiping learned and kept names. */
@Composable
internal fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.reset_progress)) },
        text = { Text(stringResource(R.string.reset_dialog_text)) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(
                    stringResource(R.string.reset),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Shown when Android will not show the permission dialog any more: without
 * it the switch would simply do nothing, for ever, with no way for the reader
 * to find out why. The confirmation hands off to system settings.
 */
@Composable
internal fun NotificationsBlockedDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.notifications_blocked_title)) },
        text = { Text(stringResource(R.string.notifications_blocked_body)) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
