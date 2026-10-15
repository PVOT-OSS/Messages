/*
 * SPDX-FileCopyrightText: 2026 Shubham-344 <shubhamanand030404@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager

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
