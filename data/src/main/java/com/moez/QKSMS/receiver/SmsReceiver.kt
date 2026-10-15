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
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.model.ReceivedSmsDetails
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.worker.ReceiveSmsWorker
import org.prauga.messages.worker.ReceiveSmsWorker.Companion.INPUT_DATA_KEY_MESSAGE_ID
import org.prauga.messages.worker.SmsSendRetryWorker
import timber.log.Timber

class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var messageRepo: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val parts =
            try {
                Sms.Intents.getMessagesFromIntent(intent)?.filterNotNull().orEmpty()
            } catch (e: Exception) {
                Timber.e(e, "Unable to read the SMS")
                emptyList()
            }
        val sms = ReceivedSms.from(parts.map(::part)) ?: return
        val subId = intent.extras?.getInt("subscription", -1) ?: -1

        val pendingResult = goAsync()
        Thread {
                try {
                    Timber.v("onReceive() new sms")
                    val messageId =
                        messageRepo
                            .insertReceivedSms(subId, sms.address, sms.body, sms.sentTime, sms.details)
                            .id

                    // start worker with message id as param
                    WorkManager.getInstance(context)
                        .enqueue(
                            OneTimeWorkRequestBuilder<ReceiveSmsWorker>()
                                .setInputData(workDataOf(INPUT_DATA_KEY_MESSAGE_ID to messageId))
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )

                    SmsSendRetryWorker.kick(context, resetDelays = true)
                } catch (e: Exception) {
                    Timber.e(e, "Unable to store the received SMS")
                } finally {
                    pendingResult.finish()
                }
            }
            .start()
    }

    private fun part(message: SmsMessage) =
        ReceivedSms.Part(
            address = tryOrNull { message.displayOriginatingAddress },
            body = tryOrNull { message.displayMessageBody },
            sentTime = tryOrNull { message.timestampMillis } ?: 0L,
            details =
                ReceivedSmsDetails(
                    protocol = tryOrNull { message.protocolIdentifier } ?: 0,
                    replyPathPresent = tryOrNull { message.isReplyPathPresent } ?: false,
                    serviceCenter = tryOrNull { message.serviceCenterAddress },
                    subject = tryOrNull { message.pseudoSubject },
                    replace = tryOrNull { message.isReplace } ?: false,
                ),
            classZero = tryOrNull { message.messageClass } == SmsMessage.MessageClass.CLASS_0,
        )

    private fun <T> tryOrNull(block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            null
        }
}
