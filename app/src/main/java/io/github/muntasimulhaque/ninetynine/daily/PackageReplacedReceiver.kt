package io.github.muntasimulhaque.ninetynine.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After an in-place update the launcher still holds the widget's OLD
 * RemoteViews, whose tap PendingIntent was created by the previous APK.
 * On Android 8.0–8.1 (observed on a Vivo Funtouch 8.1 device) the system
 * invalidates that PendingIntent when the package is replaced, so the
 * widget renders but never answers a tap — until the next app open
 * re-renders it (MainActivity.onResume → updateAll) and installs a fresh
 * PendingIntent from the new version. Re-rendering here, right after the
 * update lands, installs that fresh PendingIntent immediately.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // MY_PACKAGE_REPLACED is delivered only to the replaced package
        // itself; anything else (same-app or system) is ignored, so a
        // spoofed action cannot trigger a render.
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // goAsync keeps this process alive for the update: updateAll is a
        // suspend fun, and the system would otherwise consider the broadcast
        // handled the moment onReceive returns.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // A throw here has no exception handler and would otherwise
                // reach the uncaught-handler and kill the process. A failed
                // refresh is a skipped refresh, never a crash.
                runCatching { DailyNameWidget().updateAll(context.applicationContext) }
            } finally {
                pending.finish()
            }
        }
    }
}