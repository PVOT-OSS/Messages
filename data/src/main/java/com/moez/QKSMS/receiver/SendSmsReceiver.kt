/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.RetrySending
import org.prauga.messages.repository.MessageRepository

class SendSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var retrySending: RetrySending

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val messageId = intent.getLongExtra("id", -1L).takeIf { it >= 0 } ?: return
        val message = messageRepo.getMessage(messageId) ?: return

        val waiting = message.isSms() && message.boxId == Sms.MESSAGE_TYPE_OUTBOX
        if (!shouldSend(intent.action, message.isFailedMessage(), waiting)) return
        if (intent.action == null) messageRepo.delayedSmsFired(messageId)

        val result = goAsync()
        retrySending.execute(messageId) { result.finish() }
    }

    companion object {
        const val ACTION_RETRY = "org.prauga.messages.action.RETRY_SENDING"

        fun shouldSend(action: String?, failed: Boolean, waiting: Boolean): Boolean =
            if (action == ACTION_RETRY) failed else failed || waiting
    }
}
