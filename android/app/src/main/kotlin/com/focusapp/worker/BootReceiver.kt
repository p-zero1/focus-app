package com.focusapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Re-schedules WorkManager and AlarmManager reminders after a device reboot.
 *
 * Registered for ACTION_BOOT_COMPLETED in AndroidManifest.xml.
 * Full implementation is Phase N task T085 — stub present to satisfy manifest declarations
 * and prevent build failures while V2 reminder infrastructure is not yet wired.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO (T085): Re-schedule WorkManager periodic tasks and AlarmManager alarms
            Timber.d("BootReceiver: device rebooted — reminder re-scheduling not yet implemented")
        }
    }
}
