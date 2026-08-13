package io.github.muntasimulhaque.ninetynine.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import io.github.muntasimulhaque.ninetynine.data.NamesRepository
import io.github.muntasimulhaque.ninetynine.data.Prefs
import io.github.muntasimulhaque.ninetynine.util.DailyName
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
     * Every scheduling call here is wrapped in `runCatching`. These run at app
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
            runCatching {
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
        runCatching {
            val widgetRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutesUntil(0, 5), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_WORK,
                if (reanchor) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                else ExistingPeriodicWorkPolicy.KEEP,
                widgetRequest,
            )
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
        runCatching {
            val prefs = Prefs(context.applicationContext)
            if (!prefs.dailyEnabled.first()) return@runCatching
            val (hour, minute) = prefs.dailyTime.first()
            enqueueNotification(
                context,
                hour,
                minute,
                if (reanchor) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            )
        }
    }

    /** Applies the user's current enabled/time settings, replacing any previous schedule. */
    suspend fun rescheduleNotification(context: Context) {
        runCatching {
            val prefs = Prefs(context.applicationContext)
            if (!prefs.dailyEnabled.first()) {
                cancelNotification(context)
                return@runCatching
            }
            val (hour, minute) = prefs.dailyTime.first()
            enqueueNotification(context, hour, minute, ExistingPeriodicWorkPolicy.REPLACE)
        }
    }

    fun cancelNotification(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(NOTIFY_WORK)
        }
    }

    private fun enqueueNotification(
        context: Context,
        hour: Int,
        minute: Int,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutesUntil(hour, minute), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOTIFY_WORK, policy, request)
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
        runCatching {
            if (!isRunning(context, WIDGET_WORK)) ensureScheduled(context, reanchor = true)
            if (!isRunning(context, NOTIFY_WORK)) ensureNotificationScheduled(context, reanchor = true)
        }
    }

    /** True when the given unique work is executing right now. */
    suspend fun isRunning(context: Context, uniqueName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .getWorkInfosForUniqueWork(uniqueName)
                    .get()
            }.getOrDefault(emptyList())
                .any { it.state == WorkInfo.State.RUNNING }
        }

    /** Minutes from now until the next occurrence of hour:minute (local time). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
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
        // and a cold-start hiccup can throw outside any runCatching the
        // caller can wrap — so a throw must never kill the process. But a
        // skipped refresh is not the end of the story: this worker is the
        // widget's daily refresh, and its audience is the reader who rarely
        // opens the app at all. A transient cold-start race is exactly what
        // WorkManager's backoff retry exists for, so a failure is retried
        // rather than silently lost until tomorrow.
        return runCatching { DailyNameWidget().updateAll(applicationContext) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
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
        runCatching { DailyNameWidget().updateAll(context) }

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
            // System-font surface: use the Noto-safe form of الله (see DailyNameWidget).
            .setContentTitle("${DailyNameWidget.systemFontSafeArabic(name.arabic)}  ${name.transliteration}")
            .setContentText(name.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_tap_hint, name.title))
            )
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
        return runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1, notification)
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
