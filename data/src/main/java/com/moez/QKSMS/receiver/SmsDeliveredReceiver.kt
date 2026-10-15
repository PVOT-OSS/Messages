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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.MarkDelivered
import org.prauga.messages.interactor.MarkDeliveryFailed
import org.prauga.messages.util.tryOrNull

class SmsDeliveredReceiver : BroadcastReceiver() {

    @Inject lateinit var markDelivered: MarkDelivered
    @Inject lateinit var markDeliveryFailed: MarkDeliveryFailed

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val id = intent.getLongExtra("id", 0L)

        val status =
            intent.getByteArrayExtra("pdu")?.let { pdu ->
                val format = intent.getStringExtra("format")
                tryOrNull { SmsMessage.createFromPdu(pdu, format)?.status }
                    ?.let { SmsDeliveryStatus.from(it, format) }
            }

        when {
            // TODO notify about delivery
            status == Sms.STATUS_COMPLETE || (status == null && resultCode == Activity.RESULT_OK) -> {
                val pendingResult = goAsync()
                markDelivered.execute(id) { pendingResult.finish() }
            }

            // TODO notify about delivery failure
            status == Sms.STATUS_FAILED ||
                (status == null && resultCode == Activity.RESULT_CANCELED) -> {
                val pendingResult = goAsync()
                markDeliveryFailed.execute(
                    MarkDeliveryFailed.Params(id, status ?: resultCode)
                ) {
                    pendingResult.finish()
                }
            }
        }
    }
}
