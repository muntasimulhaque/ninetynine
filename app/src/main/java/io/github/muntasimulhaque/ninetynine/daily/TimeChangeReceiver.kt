package io.github.muntasimulhaque.ninetynine.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The daily name rolls over at local midnight (DailyName.numberFor), so a
 * timezone or DST change shifts what "today" means mid-flight — and this
 * app's audience is exactly the traveller: the reader who takes the name
 * off the home screen and rarely opens the app. Without this receiver the
 * widget would show yesterday's name until the next worker run or app open.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Only the two clock broadcasts belong here; anything else (same-app
        // or system) is ignored, so a spoofed action cannot trigger a render.
        val action = intent?.action
        if (action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return
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
