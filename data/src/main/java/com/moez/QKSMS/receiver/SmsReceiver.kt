/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.android.AndroidInjection
import java.util.concurrent.TimeUnit
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
        val subId = intent.extras?.getInt("subscription", -1) ?: -1
        if (parts.isEmpty()) return

        val pendingResult = goAsync()
        Thread {
                try {
                    receive(context, parts, subId)
                } finally {
                    pendingResult.finish()
                }
            }
            .start()
    }

    fun receive(context: Context, parts: List<SmsMessage>, subId: Int) {
        try {
            val sms = ReceivedSms.from(parts.map(::part)) ?: return
            Timber.v("onReceive() new sms")
            val messageId =
                messageRepo
                    .insertReceivedSms(subId, sms.address, sms.body, sms.sentTime, sms.details)
                    .id

            if (sms.classZero) {
                showClassZero(context, messageId)
            } else {
                // start worker with message id as param
                WorkManager.getInstance(context)
                    .enqueue(
                        OneTimeWorkRequestBuilder<ReceiveSmsWorker>()
                            .setInputData(workDataOf(INPUT_DATA_KEY_MESSAGE_ID to messageId))
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()
                    )
            }

            SmsSendRetryWorker.kick(context, resetDelays = true)
        } catch (e: Exception) {
            Timber.e(e, "Unable to store the received SMS")
        }
    }

    private fun showClassZero(context: Context, messageId: Long) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                classZeroWork(messageId),
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReceiveSmsWorker>()
                    .setInputData(workDataOf(INPUT_DATA_KEY_MESSAGE_ID to messageId))
                    .setInitialDelay(CLASS_ZERO_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .build(),
            )

        try {
            context.startActivity(
                Intent()
                    .setComponent(ComponentName(context, CLASS_ZERO_ACTIVITY))
                    .putExtra("id", messageId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Timber.w(e, "Unable to show the class 0 SMS")
        }
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

    companion object {
        private const val CLASS_ZERO_ACTIVITY =
            "org.prauga.messages.feature.classzero.ClassZeroActivity"
        private const val CLASS_ZERO_TIMEOUT_MINUTES = 5L

        fun classZeroWork(messageId: Long) = "class-zero:$messageId"
    }
}
