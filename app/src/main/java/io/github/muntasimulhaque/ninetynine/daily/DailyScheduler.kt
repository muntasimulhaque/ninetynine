package io.github.muntasimulhaque.ninetynine.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.muntasimulhaque.ninetynine.MainActivity
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.data.Name
import io.github.muntasimulhaque.ninetynine.data.NamesRepository
import io.github.muntasimulhaque.ninetynine.data.Prefs
import io.github.muntasimulhaque.ninetynine.ui.theme.HeroContainer
import io.github.muntasimulhaque.ninetynine.util.DailyName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyScheduler {

    private const val WIDGET_WORK = "daily_widget_update"
    private const val NOTIFY_WORK = "daily_notification"

    /**
     * A channel's importance can't be lowered once the system has it, so this
     * id replaces the original `name_of_the_day`, which was created at
     * IMPORTANCE_DEFAULT and therefore made a sound every morning.
     */
    const val CHANNEL_ID = "daily_name"
    private const val OLD_CHANNEL_ID = "name_of_the_day"

    /**
     * Every scheduling call here is guarded. These run at app
     * start from coroutines that have no exception handler — `lifecycleScope`
     * in MainActivity, the `applicationScope` in NamesApp — and from the UI.
     * WorkManager and DataStore are both initialised lazily on a cold start, so
     * a scheduling call can hit a transient not-yet-ready race; a throw from
     * one of these would kill the process on a launch the reader just opened.
     * A failed schedule is a degraded feature (the widget or notification skips
     * a refresh), which is preferable to a crash, and the next launch or worker
     * run retries it. Same philosophy as Prefs: a hiccup is not worth the app.
     */
    /** Creates the notification channel once at app start so users can find it in system settings. */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.deleteNotificationChannel(OLD_CHANNEL_ID)
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.daily_notification_channel),
                        // A daily invitation to reflect, not an alert: it waits in
                        // the shade rather than making a sound. Anyone who wants
                        // one can raise the channel in system settings.
                        NotificationManager.IMPORTANCE_LOW,
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Called on app start: pins the widget's refresh back to just after
     * midnight.
     *
     * CANCEL_AND_REENQUEUE, not UPDATE. UPDATE carries the old work's enqueue
     * time and period count across to the replacement, and WorkManager only
     * honours an initial delay before the first period has completed — so
     * after the first run the delay set here is ignored and a schedule that
     * has drifted under Doze stays drifted. The people this matters to are
     * exactly the widget's audience: readers who take the day's name off the
     * home screen and rarely open the app at all.
     */
    fun ensureScheduled(context: Context, reanchor: Boolean) {
        try {
            val widgetRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutesUntil(0, 5), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_WORK,
                if (reanchor) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                else ExistingPeriodicWorkPolicy.KEEP,
                widgetRequest,
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Re-arms the daily notification at app start if the user has it enabled.
     *
     * Deliberately REPLACE, not KEEP: WorkManager schedules each period from
     * when the previous one actually ran, so every Doze deferral pushes the
     * notification a little later and the error accumulates with nothing to
     * correct it. Re-anchoring on launch pins it back to the chosen time, and
     * fixes a timezone or DST change at the same time.
     */
    suspend fun ensureNotificationScheduled(context: Context, reanchor: Boolean) {
        try {
            val prefs = Prefs(context.applicationContext)
            if (!prefs.dailyEnabled.first()) return
            val (hour, minute) = prefs.dailyTime.first()
            enqueueNotification(
                context,
                hour,
                minute,
                if (reanchor) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** Applies the user's current enabled/time settings, replacing any previous schedule. */
    suspend fun rescheduleNotification(context: Context) {
        try {
            val prefs = Prefs(context.applicationContext)
            if (!prefs.dailyEnabled.first()) {
                cancelNotification(context)
                return
            }
            val (hour, minute) = prefs.dailyTime.first()
            enqueueNotification(context, hour, minute, ExistingPeriodicWorkPolicy.REPLACE)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    fun cancelNotification(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(NOTIFY_WORK)
        } catch (_: Exception) {
        }
    }

    private fun enqueueNotification(
        context: Context,
        hour: Int,
        minute: Int,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        try {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutesUntil(hour, minute), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOTIFY_WORK, policy, request)
        } catch (_: Exception) {
        }
    }

    /**
     * Re-anchors both schedules unless their unique work is mid-run.
     *
     * Opening the app in the seconds a worker is executing must not cancel it
     * (CANCEL_AND_REENQUEUE/REPLACE cancel a running worker, and the
     * replacement's initial delay is ~24h out) — the same failure the
     * Application.onCreate re-anchor had, with a seconds-wide window instead
     * of an unconditional one.
     */
    suspend fun reanchorSchedules(context: Context) {
        try {
            if (!isRunning(context, WIDGET_WORK)) ensureScheduled(context, reanchor = true)
            if (!isRunning(context, NOTIFY_WORK)) ensureNotificationScheduled(context, reanchor = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** True when the given unique work is executing right now. */
    suspend fun isRunning(context: Context, uniqueName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                WorkManager.getInstance(context.applicationContext)
                    .getWorkInfosForUniqueWork(uniqueName)
                    .get()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Unknown beats wrong: a lookup failure must not read as
                // "idle" and cancel work that may be mid-run. Callers skip
                // the re-anchor when this is true, which is the safe side.
                return@withContext true
            }
                .any { it.state == WorkInfo.State.RUNNING }
        }

    /** Minutes from now until the next occurrence of hour:minute (local time). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (next.timeInMillis - now.timeInMillis) / 60_000L + 1
    }
}

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // The render runs inside a Glance SessionWorker on the main thread,
        // and a cold-start hiccup can throw outside any guard the
        // caller can wrap — so a throw must never kill the process. But a
        // skipped refresh is not the end of the story: this worker is the
        // widget's daily refresh, and its audience is the reader who rarely
        // opens the app at all. A transient cold-start race is exactly what
        // WorkManager's backoff retry exists for, so a failure is retried
        // rather than silently lost until tomorrow.
        return try {
            DailyNameWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        // Same rule as WidgetUpdateWorker: a cold-start hiccup is a
        // skipped refresh, never a crash. The widget has its own worker
        // at 00:05, so a failed nudge here is not worth a retry of its
        // own — the notification below is the reason this worker ran.
        try {
            DailyNameWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val names = NamesRepository.load(context)
        val name = names.firstOrNull { it.number == DailyName.numberFor(System.currentTimeMillis()) }
            ?: return Result.success()

        // NEW_TASK only: the activity is singleTop and consumes the extra, so
        // this lands on the name without tearing down whatever was open.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAME_NUMBER, name.number)
        }
        val pending = PendingIntent.getActivity(
            context, name.number, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DailyScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // Many skins tint the small icon's backdrop circle with this —
            // the app's own emerald, so even the shade carries the identity.
            .setColor(HeroContainer.toArgb())
            // A daily invitation to read, not an alarm or a calendar event.
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // System-font surface: use the Noto-safe form of الله (see DailyNameWidget).
            // The middle dot is the same separator the feature graphic's tagline
            // wears — the shade's one line of typography, set rather than joined.
            .setContentTitle("${DailyNameWidget.systemFontSafeArabic(name.arabic)} · ${name.transliteration}")
            .setContentText(name.title)
            .setStyle(dailyStyle(context, name))
            .setContentIntent(pending)
            .setAutoCancel(true)
            // Channels only exist from API 26; on 24–25 the priority is what
            // decides whether this makes a sound.
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Permission checked above; the channel is created in NamesApp.onCreate.
        // A transient posting failure is retried with WorkManager's backoff —
        // the daily notification is the one surface a reader who never opens
        // the app sees, so a silently skipped morning is the worst outcome.
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1, notification)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

/**
 * The expanded form: the emerald plate when it renders, the plain text when
 * it does not. Collapsed, the notification is unchanged either way — the
 * picture only appears once the reader pulls the shade down and expands it.
 *
 * With the plate up, the summary is the bare tap hint: the plate already
 * carries the short meaning, and repeating it made the expanded shade read
 * as the old text notification sitting on top of a duplicate of itself. The
 * header line above the plate (Arabic · transliteration) is the collapsed
 * view itself, which the template always shows and nothing can remove.
 *
 * Without the plate the short meaning has nowhere else to be, so the
 * fallback keeps the full line.
 */
private fun dailyStyle(context: Context, name: Name): NotificationCompat.Style {
    val plate = DailyPlate.render(context, name)
    return if (plate != null) {
        NotificationCompat.BigPictureStyle()
            .bigPicture(plate)
            .setSummaryText(context.getString(R.string.notification_summary_hint))
    } else {
        NotificationCompat.BigTextStyle()
            .bigText(context.getString(R.string.notification_tap_hint, name.title))
    }
}
