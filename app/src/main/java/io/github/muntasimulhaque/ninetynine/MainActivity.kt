package io.github.muntasimulhaque.ninetynine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import io.github.muntasimulhaque.ninetynine.daily.DailyNameWidget
import io.github.muntasimulhaque.ninetynine.daily.DailyScheduler
import io.github.muntasimulhaque.ninetynine.data.Prefs
import io.github.muntasimulhaque.ninetynine.ui.App
import io.github.muntasimulhaque.ninetynine.util.DailyName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private var startNumber by mutableIntStateOf(-1)

    /** The launcher-shortcut deep link, if this launch came from one. */
    private var startRoute by mutableStateOf<String?>(null)

    /** False until Compose's first composition has committed; holds the splash. */
    private var contentReady = false

    /**
     * False until the stored theme and text scale have been read from
     * DataStore (bounded: see [THEME_SETTLE_TIMEOUT_MILLIS]). The splash
     * holds past the first frame until this turns true. Without it, the
     * flows' defaults (SYSTEM / 1.0) compose the first frame while DataStore
     * is still reading, and a DARK or BLACK reader on a light system saw the
     * app flash light before its real theme landed: a wrong-theme frame
     * committed in the open. With the gate, the first frame the reader sees
     * is the theme they chose.
     */
    private var themeSettled = false

    /**
     * The local day the widget last got its onResume nudge for. The nudge
     * exists so a phone opened across midnight never shows the widget
     * yesterday's name; within one day, re-rendering on every resume only
     * repeated a full Glance composition on the main thread for a card that
     * already shows today. The daily-name worker at 00:05 remains the real
     * refresher, and process death resets this, so the first resume after a
     * cold start always renders.
     */
    private var widgetNudgeDay: Int = Int.MIN_VALUE

    /**
     * The one-time ask for POST_NOTIFICATIONS. Registered at construction:
     * the contract requires that to happen before the activity is STARTED.
     * The reminder itself is on by default (see [maybeAskForNotifications]);
     * a denial writes the pref off, so the Settings switch, the scheduler and
     * the worker all agree that nothing should be posted.
     */
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) onReminderPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Hold the system splash until the app's first frame is committed
        // AND the stored theme has landed (see [themeSettled]). Without
        // this, a slow device can dismiss the splash a frame or two
        // before Compose draws (a flash of bare window background between
        // splash and app), and without the theme gate, the first committed
        // frame is the flows' defaults, not the reader's choice. The
        // condition is polled every frame on the main
        // thread, so plain booleans are enough; SideEffect fires exactly when
        // the first composition has been applied, and the settle flag turns
        // when DataStore's first values arrive, the splash releases onto a
        // frame already in the reader's theme.
        splashScreen.setKeepOnScreenCondition { !contentReady || !themeSettled }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read the stored theme and text scale once, bounded: DataStore
        // normally answers within a few milliseconds of the first read, and
        // the ViewModel's Eagerly flows share the same store, so this wait
        // and the first real values arrive together. The bound keeps a
        // pathological store from holding the splash for ever, past the
        // timeout the defaults compose, exactly as before this gate existed.
        lifecycleScope.launch {
            withTimeoutOrNull(THEME_SETTLE_TIMEOUT_MILLIS) {
                val prefs = Prefs(applicationContext)
                prefs.themeMode.first()
                prefs.textScale.first()
            }
            themeSettled = true
        }

        // Re-anchor here, not in Application.onCreate. This runs only when a
        // person actually opens the app, so it cannot cancel a worker that
        // WorkManager just started the process to run. Doze deferrals still get
        // corrected; every launch pins both schedules back to their times.
        // The running-check keeps even the seconds-wide window from cancelling
        // a worker that is mid-run at the instant the app opens.
        lifecycleScope.launch { DailyScheduler.reanchorSchedules(this@MainActivity) }

        // The reminder is on by default; on 13+ its consent is the system
        // permission dialog, so a fresh install is asked once, here. Details
        // in [maybeAskForNotifications].
        lifecycleScope.launch { maybeAskForNotifications() }

        // Process death replays the ORIGINAL launch intent (the removal below
        // never propagates to the system's ActivityRecord), so a restored
        // activity would force-navigate to the deep-linked name again. The
        // extra was already consumed and navigated before the death.
        if (savedInstanceState == null) {
            startNumber = consumeNameNumber(intent)
            startRoute = consumeStartRoute(intent)
        }
        setContent {
            SideEffect { contentReady = true }
            App(
                startNumber,
                startRoute,
                onStartNumberConsumed = { startNumber = -1 },
                onStartRouteConsumed = { startRoute = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startNumber = consumeNameNumber(intent)
        startRoute = consumeStartRoute(intent)
    }

    /**
     * The home screen recomputes the daily name on every resume; the widget
     * only refreshes when its worker runs. Nudging it here keeps the two
     * surfaces from showing different names after midnight: gated to a real
     * day change, so an ordinary return to the app costs no render (see
     * [widgetNudgeDay]).
     */
    override fun onResume() {
        super.onResume()
        val today = DailyName.numberFor(System.currentTimeMillis())
        if (today == widgetNudgeDay) return
        widgetNudgeDay = today
        // Glance's updateAll can race its own initialisation on a cold start;
        // a throw here would kill the process the reader just opened. The
        // widget refreshes on its own worker run anyway, so a failed nudge is
        // a skipped refresh, never a crash.
        lifecycleScope.launch {
            try {
                DailyNameWidget().updateAll(this@MainActivity)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Asks for POST_NOTIFICATIONS once, on the first launch of a reader who
     * wants the reminder (it is on by default). The ask only fires when all
     * of these hold:
     *
     * - API 33+: below that there is no runtime permission to ask for, and
     *   the reminder simply works: the platform's own default for every
     *   installed app.
     * - the permission is not already granted (via system settings or a
     *   restore).
     * - it has never been asked before, on any launch: the flag is written
     *   before the dialog opens, so a process death mid-dialog never nags.
     * - the reminder is actually wanted: a reader whose pref says off
     *   (toggled off, or a denial from an earlier ask) is never asked.
     *
     * A grant needs no wiring: the schedule re-anchors on every launch and
     * the worker re-checks the permission at post time. A denial writes the
     * pref off and cancels the scheduled work, so the Settings switch shows
     * the truth and no worker wakes to no-op.
     */
    private suspend fun maybeAskForNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = Prefs(applicationContext)
        if (prefs.notificationsAsked.first()) return
        if (!prefs.dailyEnabled.first()) return
        prefs.setNotificationsAsked()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** The reader said no to the one ask: the reminder follows them to off. */
    private fun onReminderPermissionDenied() {
        lifecycleScope.launch {
            Prefs(applicationContext).setDailyEnabled(false)
            DailyScheduler.cancelNotification(applicationContext)
        }
    }

    /** Reads the extra, then removes it so a configuration change can't replay the navigation. */
    private fun consumeNameNumber(intent: Intent?): Int {
        // This activity is exported, so any app on the device can launch it
        // with arbitrary extras. Reading one unparcels the whole bundle, and a
        // crafted extra naming a class this app does not have throws
        // BadParcelableException right here in onCreate. The removal runs in
        // a finally so a poisoned extra cannot survive to throw again.
        try {
            return intent?.getIntExtra(EXTRA_NAME_NUMBER, -1) ?: -1
        } catch (_: Exception) {
            return -1
        } finally {
            try {
                intent?.removeExtra(EXTRA_NAME_NUMBER)
            } catch (_: Exception) {
            }
        }
    }

    /** Same consume-and-remove discipline as [consumeNameNumber], for shortcut routes. */
    private fun consumeStartRoute(intent: Intent?): String? {
        try {
            return intent?.getStringExtra(EXTRA_START_ROUTE)
        } catch (_: Exception) {
            return null
        } finally {
            try {
                intent?.removeExtra(EXTRA_START_ROUTE)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        /**
         * The longest the splash may wait for the stored theme. A warm read
         * lands in tens of milliseconds; four hundred covers cold storage
         * without ever reading as a hang.
         */
        private const val THEME_SETTLE_TIMEOUT_MILLIS = 400L

        const val EXTRA_NAME_NUMBER = "nameNumber"

        /** Carried by the launcher shortcuts (res/xml/shortcuts.xml). */
        const val EXTRA_START_ROUTE = "startRoute"
    }
}
