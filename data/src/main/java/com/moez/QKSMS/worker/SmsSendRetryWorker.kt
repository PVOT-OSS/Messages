/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import android.provider.Telephony.Sms
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import org.prauga.messages.interactor.RetrySending
import org.prauga.messages.repository.MessageRepository
import timber.log.Timber

class SmsSendRetryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    lateinit var messageRepo: MessageRepository
    lateinit var retrySending: RetrySending

    override fun doWork(): Result {
        val messageId =
            inputData.getLong(KEY_MESSAGE_ID, -1).takeIf { it != -1L } ?: return Result.failure()

        val message = messageRepo.getMessage(messageId)
        if (message == null || message.boxId != Sms.MESSAGE_TYPE_OUTBOX) {
            clear(applicationContext, messageId)
            return Result.success()
        }

        Timber.v("Retrying to send SMS $messageId")
        inFlightPrefs(applicationContext).edit().putBoolean(messageId.toString(), true).commit()
        retrySending.buildObservable(messageId).blockingSubscribe({}, Timber::w)
        return Result.success()
    }

    companion object {
        private const val KEY_MESSAGE_ID = "messageId"

        private fun backoff(context: Context) = MmsRetryBackoff(context, "sms_send_retries")

        private fun inFlightPrefs(context: Context) =
            context.getSharedPreferences("sms_send_retries_in_flight", Context.MODE_PRIVATE)

        fun schedule(context: Context, messageId: Long): Boolean {
            inFlightPrefs(context).edit().remove(messageId.toString()).apply()
            val delay = backoff(context).nextDelay(messageId.toString()) ?: return false
            enqueue(context, messageId, delay)
            SmsServiceWatcher.watch(context)
            return true
        }

        fun kick(context: Context, resetDelays: Boolean) {
            val backoff = backoff(context)
            val sending = inFlightPrefs(context).all.keys
            pending(context)
                .filter { it.toString() !in sending }
                .forEach { messageId ->
                    if (resetDelays) backoff.reset(messageId.toString())
                    enqueue(context, messageId, 0)
                }
        }

        private fun enqueue(context: Context, messageId: Long, delay: Long) {
            val request =
                OneTimeWorkRequestBuilder<SmsSendRetryWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("sms-send-retry:$messageId", ExistingWorkPolicy.REPLACE, request)
        }

        fun clear(context: Context, messageId: Long) {
            inFlightPrefs(context).edit().remove(messageId.toString()).apply()
            backoff(context).clear(messageId.toString())
            if (pending(context).isEmpty()) SmsServiceWatcher.unwatch()
        }

        fun pending(context: Context): Set<Long> =
            backoff(context).keys().mapNotNull(String::toLongOrNull).toSet()

        fun inFlight(context: Context): Set<Long> =
            inFlightPrefs(context).all.keys.mapNotNull(String::toLongOrNull).toSet()
    }
}
