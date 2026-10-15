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
