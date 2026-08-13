package io.github.muntasimulhaque.ninetynine.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * After an in-place update the launcher still holds the widget's OLD
 * RemoteViews, whose tap PendingIntent was created by the previous APK.
 * On Android 8.0–8.1 (observed on a Vivo Funtouch 8.1 device) the system
 * invalidates that PendingIntent when the package is replaced, so the
 * widget renders but never answers a tap — until the next app open
 * re-renders it (MainActivity.onResume → updateAll) and installs a fresh
 * PendingIntent from the new version.
 *
 * This receiver enqueues a one-time WidgetUpdateWorker via WorkManager
 * instead of using goAsync() with a raw coroutine. WorkManager is more
 * reliable because it persists the work request to its database — even if
 * the process is killed after this receiver returns, the update will still
 * execute when the system restarts the process. The worker re-renders all
 * widget instances, installing fresh PendingIntents from the new APK.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // MY_PACKAGE_REPLACED is delivered only to the replaced package
        // itself; anything else (same-app or system) is ignored, so a
        // spoofed action cannot trigger a render.
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // Enqueue an immediate one-time widget update via WorkManager.
        // WorkManager persists the request to its database, so even if the
        // process is killed after this receiver returns, the update will
        // execute when the process restarts — unlike goAsync() which has a
        // 10-second lease and no persistence across process death.
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "post_update_widget_refresh",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}