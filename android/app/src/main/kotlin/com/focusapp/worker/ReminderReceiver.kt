package com.focusapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Fires AlarmManager habit-reminder notifications.
 *
 * Registered in AndroidManifest.xml. Full implementation is Phase 8 (V2) task T077 —
 * stub present to satisfy manifest declarations while V2 reminder infrastructure
 * is not yet implemented.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO (T077 / V2): Post habit reminder notification and re-schedule next occurrence
        Timber.d("ReminderReceiver: received — habit reminder not yet implemented")
    }
}
