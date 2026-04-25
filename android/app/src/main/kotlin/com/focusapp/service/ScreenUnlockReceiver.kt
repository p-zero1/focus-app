package com.focusapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dynamic [BroadcastReceiver] registered by [TimerService] during an active session.
 * Receives ACTION_USER_PRESENT (screen unlocked after PIN/swipe) and forwards the
 * event to [DistractionMonitor] via the [onUnlock] callback.
 *
 * Registered/unregistered programmatically — NOT declared in the manifest —
 * so it only fires when a session is in progress.
 */
class ScreenUnlockReceiver(
    private val onUnlock: () -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            onUnlock()
        }
    }
}
