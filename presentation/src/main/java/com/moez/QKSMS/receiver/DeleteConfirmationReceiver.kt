/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
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
