package io.github.muntasimulhaque.ninetynine.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * The daily name rolls over at local midnight (DailyName.numberFor), so a
 * timezone or DST change shifts what "today" means mid-flight — and this
 * app's audience is exactly the traveller: the reader who takes the name
 * off the home screen and rarely opens the app. Without this receiver the
 * widget would show yesterday's name until the next worker run or app open.
 *
 * Uses WorkManager instead of goAsync() with a raw coroutine for the same
 * reliability reasons as PackageReplacedReceiver: WorkManager persists the
 * work request and survives process death, so the widget update is guaranteed
 * to run even if the system kills the broadcast receiver's process early.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Only the two clock broadcasts belong here; anything else (same-app
        // or system) is ignored, so a spoofed action cannot trigger a render.
        val action = intent?.action
        if (action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return

        // Enqueue an immediate one-time widget update via WorkManager.
        // WorkManager persists the request to its database, so even if the
        // process is killed after this receiver returns, the update will
        // execute when the process restarts — unlike goAsync() which has a
        // 10-second lease and no persistence across process death.
        //
        // Guarded, like every other scheduler call site: WorkManager
        // initialises lazily, and a transient init race throwing from inside
        // onReceive would kill the process mid-broadcast — at exactly the
        // moments (clock change, app update) a refresh is least affordable.
        // A skipped refresh retries on the next worker run; a crash does not.
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .build()
        try {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "time_change_widget_refresh",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        } catch (_: Exception) {
        }
    }
}
