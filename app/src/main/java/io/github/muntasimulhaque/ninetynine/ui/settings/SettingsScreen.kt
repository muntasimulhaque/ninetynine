package io.github.muntasimulhaque.ninetynine.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.ninetynine.BuildConfig
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.ThemeMode
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.theme.BlackColors
import io.github.muntasimulhaque.ninetynine.ui.theme.DarkColors
import io.github.muntasimulhaque.ninetynine.ui.theme.LightColors
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDeviceFactor
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.appTypography
import io.github.muntasimulhaque.ninetynine.ui.theme.rememberHaptics
import io.github.muntasimulhaque.ninetynine.ui.theme.components.BackButton
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ListInset
import io.github.muntasimulhaque.ninetynine.ui.theme.components.MixedText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.PageRule
import io.github.muntasimulhaque.ninetynine.ui.theme.components.ScreenLabel
import io.github.muntasimulhaque.ninetynine.ui.theme.components.pageMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.paperTopBarColors
import io.github.muntasimulhaque.ninetynine.ui.theme.components.SectionLabel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val SCALE_MIN = 0.85f
private const val SCALE_MAX = 1.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NamesViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val textScale by viewModel.textScale.collectAsStateWithLifecycle()
    val dailyEnabled by viewModel.dailyEnabled.collectAsStateWithLifecycle()
    val dailyTime by viewModel.dailyTime.collectAsStateWithLifecycle()
    val dailyTimeLoaded by viewModel.dailyTimeLoaded.collectAsStateWithLifecycle()

    var sliderValue by remember(textScale) { mutableFloatStateOf(textScale) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showBlockedDialog by rememberSaveable { mutableStateOf(false) }
    // Set when the reader tries to turn the reminder on and the permission is
    // blocked: the dialog takes them to system settings, and if they grant it
    // there, resuming this screen completes the flip they originally asked
    // for — otherwise the switch would silently need a second toggle.
    var pendingEnable by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Say something on denial. Android stops showing the system dialog
        // after the second refusal, so without this the switch would simply do
        // nothing, for ever, with no way for the reader to find out why.
        if (granted) {
            haptics.tick()
            viewModel.setDailyEnabled(true)
        } else {
            pendingEnable = true
            showBlockedDialog = true
        }
    }

    // The permission can also be withdrawn in system settings long after it was
    // granted, which would leave this screen saying the reminder is on while
    // nothing is ever posted. And the reverse: granted in system settings after
    // the blocked dialog, which should finish the enable the reader asked for.
    LifecycleResumeEffect(dailyEnabled, pendingEnable) {
        if (pendingEnable && notificationsAllowed(context)) {
            pendingEnable = false
            haptics.tick()
            viewModel.setDailyEnabled(true)
        }
        if (dailyEnabled && !notificationsAllowed(context)) viewModel.setDailyEnabled(false)
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            // A pushed screen since the gear replaced the tab, so it is titled
            // and left the same way as About, Flashcards and Quiz.
            TopAppBar(
                colors = paperTopBarColors(),
                title = { ScreenLabel(stringResource(R.string.settings)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Wide screens keep the book's column: the settings hold page
                // proportions instead of stretching edge to edge. Phones
                // never reach the cap.
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = pageMeasure())
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ListInset),
        ) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(stringResource(R.string.theme_section))
            Spacer(Modifier.height(10.dp))
            Column(Modifier.selectableGroup()) {
                // A featherweight tick on choice, matching every other
                // meaningful toggle in the app — the switch, the bookmark,
                // the quiz answer all speak to the finger.
                ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, themeMode) {
                    haptics.tick()
                    viewModel.setThemeMode(it)
                }
                ThemeOption(ThemeMode.LIGHT, R.string.theme_light, themeMode) {
                    haptics.tick()
                    viewModel.setThemeMode(it)
                }
                ThemeOption(ThemeMode.DARK, R.string.theme_dark, themeMode) {
                    haptics.tick()
                    viewModel.setThemeMode(it)
                }
                ThemeOption(ThemeMode.BLACK, R.string.theme_black, themeMode) {
                    haptics.tick()
                    viewModel.setThemeMode(it)
                }
            }

            SectionBreak()
            SectionLabel(stringResource(R.string.text_size))
            Spacer(Modifier.height(16.dp))
            // The specimen itself is the preview — no box around it — and it
            // answers the bead mid-drag, not only after release: set at the
            // slider's CURRENT value × the device factor (absolute, via
            // appTypography, not the theme's committed scale), so the reader
            // sees exactly the size they are choosing — at the size this
            // device sets the book — before it is written to DataStore.
            val deviceFactor = LocalDeviceFactor.current
            val previewStyle = remember(sliderValue, deviceFactor) {
                appTypography(sliderValue * deviceFactor).headlineMedium
            }
            MixedText(
                text = stringResource(R.string.text_size_preview),
                style = previewStyle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
            HairlineSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.setTextScale(sliderValue) },
            )

            // The slider's own touch target already leaves air beneath the track.
            SectionBreak(top = 6.dp)
            SectionLabel(stringResource(R.string.daily_section))
            // A little more air than the label-only rows: the switch row is
            // taller and carries a control.
            Spacer(Modifier.height(8.dp))
            // Toggling lives on the row, not the Switch: a bare Switch has no
            // accessible name of its own, because its label is a sibling.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = dailyEnabled,
                        role = Role.Switch,
                        onValueChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= 33) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // The same featherweight tick every other
                                // toggle in the app answers with.
                                haptics.tick()
                                viewModel.setDailyEnabled(enable)
                            }
                        },
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.daily_reminder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = dailyEnabled, onCheckedChange = null)
            }
            AnimatedVisibility(
                visible = dailyEnabled,
                enter = fadeIn(Motion.tween(Motion.GENTLE)) + expandVertically(Motion.tween(Motion.GENTLE)),
                exit = fadeOut(Motion.tween(Motion.QUICK)) + shrinkVertically(Motion.tween(Motion.QUICK)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Not openable until DataStore delivers the real time:
                        // the picker's remembered state seeds from the 8:00
                        // default and never re-seeds, so opening it early
                        // would silently overwrite the user's chosen time.
                        .clickable(
                            enabled = dailyTimeLoaded,
                            onClickLabel = stringResource(R.string.cd_change_time),
                        ) { showTimePicker = true }
                        // The row is inert while DataStore has not delivered
                        // the saved time; say so, or a screen reader hears a
                        // plain row that later becomes a button.
                        .semantics { if (!dailyTimeLoaded) disabled() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.reminder_time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTime(context, dailyTime.first, dailyTime.second),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            SectionBreak()
            SectionLabel(stringResource(R.string.memorization_section))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reset_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // About used to sit here as a row. It is reached from the ⓘ in
            // every tab bar instead: it is the book's front matter — the
            // hadith, the source, the typefaces — which is content, and this
            // page is configuration. Same argument that moved Settings itself
            // out of the bottom bar and into a corner.
            SectionBreak()
            // Compile-time constant: no PackageManager call, and no fallback
            // string to go stale one release after somebody forgets it.
            // A datum, not a heading. SectionLabel is the app's heading style, so
            // this rendered "VERSION 3.3" as a gold section label with no
            // section under it — and the only gold on the page announcing
            // nothing. Same treatment as the closing line on About.
            Text(
                text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showTimePicker) {
        // Material 3 time picker, themed with the app — not the legacy dialog.
        val timeState = rememberTimePickerState(
            initialHour = dailyTime.first,
            initialMinute = dailyTime.second,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = {
                TimePicker(state = timeState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDailyTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_progress)) },
            text = { Text(stringResource(R.string.reset_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProgress()
                    showResetDialog = false
                }) {
                    Text(
                        stringResource(R.string.reset),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(stringResource(R.string.notifications_blocked_title)) },
            text = { Text(stringResource(R.string.notifications_blocked_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBlockedDialog = false
                    openNotificationSettings(context)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** Whether Android will actually let the daily name be posted. */
private fun notificationsAllowed(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** Takes the reader to the one place a blocked permission can be granted again. */
private fun openNotificationSettings(context: android.content.Context) {
    val toNotifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    val toAppDetails = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    // Both are hand-offs to the system; a device with neither should not crash.
    runCatching { context.startActivity(toNotifications) }
        .onFailure { runCatching { context.startActivity(toAppDetails) } }
}

/**
 * The space and rule that separate one group of choices from the next.
 * Controls that carry their own touch padding pass a smaller [top].
 */
@Composable
private fun SectionBreak(top: Dp = 30.dp, bottom: Dp = 30.dp) {
    Spacer(Modifier.height(top))
    PageRule()
    Spacer(Modifier.height(bottom))
}

/**
 * One theme, chosen typographically: the current one steps up in weight and
 * ink and takes a gold check. No radio, no container.
 */
@Composable
private fun ThemeOption(
    mode: ThemeMode,
    labelRes: Int,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val selected = current == mode
    // Fading the check keeps the row from shifting as the choice moves.
    val checkAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.tween(Motion.QUICK),
        label = "themeCheck",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelect(mode) },
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(mode)
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { alpha = checkAlpha },
        )
    }
}

/**
 * A theme in miniature: its paper, and its ink as a bead — the eye picks
 * before the mind reads. System wears both papers split, because it is
 * whichever the device is in; its bead follows the theme actually rendering.
 * Purely visual: the row above carries the name and the state for readers.
 */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val ink = when (mode) {
        ThemeMode.LIGHT -> LightColors.primary
        ThemeMode.DARK, ThemeMode.BLACK -> DarkColors.primary
        ThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (mode) {
            ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(LightColors.background))
                Box(Modifier.weight(1f).fillMaxHeight().background(DarkColors.background))
            }
            ThemeMode.LIGHT -> Box(Modifier.fillMaxSize().background(LightColors.background))
            ThemeMode.DARK -> Box(Modifier.fillMaxSize().background(DarkColors.background))
            ThemeMode.BLACK -> Box(Modifier.fillMaxSize().background(BlackColors.background))
        }
        // A 1dp mat of the page's own surface around the bead: the ink never
        // touches either paper directly, so it reads cleanly even on System's
        // split circle — where the bead straddles light and dark halves at once.
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(8.dp).background(ink, CircleShape))
        }
    }
}

/** A gold bead on a hairline — the Material slider stripped to the app's line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HairlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val gold = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.outlineVariant
    val fraction = ((value - SCALE_MIN) / (SCALE_MAX - SCALE_MIN)).coerceIn(0f, 1f)
    val label = stringResource(R.string.text_size)
    val percent = stringResource(R.string.percent, (value * 100).roundToInt())
    // The custom thumb below replaces Material's whole thumb slot, which is
    // where its focus ring was drawn — so the ring has to be drawn by hand
    // here, or keyboard users get no indication at all on the one control
    // in the app that takes keyboard input.
    var focused by remember { mutableStateOf(false) }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = SCALE_MIN..SCALE_MAX,
        // A bare Slider announces "seek control, 27 percent" with no subject
        // and no unit; the hairline also leaves only a 16dp focus rectangle.
        modifier = Modifier
            .heightIn(min = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = label
                stateDescription = percent
            },
        thumb = {
            Box(
                Modifier
                    .size(14.dp)
                    .background(gold, CircleShape)
                    .then(
                        if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
            )
        },
        track = { _ ->
            Box(Modifier.fillMaxWidth().height(1.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(track)
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(1.dp)
                        .background(gold)
                )
            }
        },
    )
}

private fun formatTime(context: android.content.Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
}
