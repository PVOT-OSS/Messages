/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

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
import timber.log.Timber

class SmsDeliveredReceiver : BroadcastReceiver() {

    @Inject lateinit var markDelivered: MarkDelivered
    @Inject lateinit var markDeliveryFailed: MarkDeliveryFailed

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val id = intent.getLongExtra("id", 0L)

        val pdu = intent.getByteArrayExtra("pdu")
        if (pdu == null) {
            Timber.w("Empty delivery report for $id")
            return
        }
        val format = intent.getStringExtra("format")
        val status =
            tryOrNull { SmsMessage.createFromPdu(pdu, format)?.status }
                ?.let { SmsDeliveryStatus.from(it, format) }

        when (status) {
            // TODO notify about delivery
            Sms.STATUS_COMPLETE -> {
                val pendingResult = goAsync()
                markDelivered.execute(id) { pendingResult.finish() }
            }

            // TODO notify about delivery failure
            Sms.STATUS_FAILED -> {
                val pendingResult = goAsync()
                markDeliveryFailed.execute(MarkDeliveryFailed.Params(id, status)) {
                    pendingResult.finish()
                }
            }
        }
    }
}
