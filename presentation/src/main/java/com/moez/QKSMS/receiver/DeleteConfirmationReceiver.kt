/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import org.prauga.messages.manager.NotificationManager
import javax.inject.Inject

class DeleteConfirmationReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val threadId = intent.getLongExtra("threadId", 0)
        val action = intent.getStringExtra("action") ?: return

        when (action) {
            "show_confirmation" -> {
                // Update notification to show confirmation
                notificationManager.showDeleteConfirmation(threadId)
            }
            "cancel" -> {
                // Restore original notification
                notificationManager.update(threadId)
            }
        }
    }
}
