/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.DeleteMessages

class DeleteMessagesReceiver : BroadcastReceiver() {

    @Inject lateinit var deleteMessages: DeleteMessages

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val pendingResult = goAsync()
        val threadId = intent.getLongExtra("threadId", 0)
        val messageIds = intent.getLongArrayExtra("messageIds") ?: longArrayOf()
        deleteMessages.execute(DeleteMessages.Params(messageIds.toList(), threadId)) {
            pendingResult.finish()
        }
    }
}
