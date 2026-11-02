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
        deleteMessages.execute(params(intent)) { pendingResult.finish() }
    }

    companion object {
        const val EXTRA_UNSEEN_UNTIL = "unseenUntil"

        fun params(intent: Intent): DeleteMessages.Params {
            val threadId = intent.getLongExtra("threadId", 0)
            val messageIds = intent.getLongArrayExtra("messageIds")
            return when {
                messageIds != null -> DeleteMessages.Params(messageIds.toList(), threadId)
                intent.hasExtra(EXTRA_UNSEEN_UNTIL) ->
                    DeleteMessages.Params(
                        emptyList(),
                        threadId,
                        unseenUntil = intent.getLongExtra(EXTRA_UNSEEN_UNTIL, 0),
                    )
                else -> DeleteMessages.Params(emptyList(), threadId)
            }
        }
    }
}
