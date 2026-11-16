/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.ReceiveSms
import org.prauga.messages.model.ReceivedSmsDetails
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.worker.ReceiveSmsWorker
import org.prauga.messages.worker.ReceiveSmsWorker.Companion.INPUT_DATA_KEY_MESSAGE_ID
import org.prauga.messages.worker.SmsSendRetryWorker
import timber.log.Timber

class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var receiveSms: ReceiveSms

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
                receiveClassZero(context, messageId)
            } else {
                // start worker with message id as param
                WorkManager.getInstance(context)
                    .enqueue(
                        OneTimeWorkRequestBuilder<ReceiveSmsWorker>()
                            .setInputData(workDataOf(INPUT_DATA_KEY_MESSAGE_ID to messageId))
                            .apply {
                                // Below Android 12, expedited work must run as a foreground service
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                }
                            }
                            .build()
                    )
            }

            SmsSendRetryWorker.kick(context, resetDelays = true)
        } catch (e: Exception) {
            Timber.e(e, "Unable to store the received SMS")
        }
    }

    /**
     * Runs before the broadcast finishes: the platform lets the default SMS app start an activity
     * from the background only while it handles a class 0 delivery
     */
    internal fun receiveClassZero(context: Context, messageId: Long) {
        receiveSms.buildObservable(messageId).blockingSubscribe({}, Timber::w)

        // Blocking or a content filter may have dropped it
        val threadId = messageRepo.getMessage(messageId)?.threadId ?: return
        if (conversationRepo.getConversation(threadId)?.blocked == true) return

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
    }
}
