/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
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
        retrySending.buildObservable(messageId).blockingSubscribe({}, Timber::w)
        return Result.success()
    }

    companion object {
        private const val KEY_MESSAGE_ID = "messageId"

        private fun backoff(context: Context) = MmsRetryBackoff(context, "sms_send_retries")

        fun schedule(context: Context, messageId: Long): Boolean {
            val delay = backoff(context).nextDelay(messageId.toString()) ?: return false

            val request =
                OneTimeWorkRequestBuilder<SmsSendRetryWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("sms-send-retry:$messageId", ExistingWorkPolicy.REPLACE, request)
            return true
        }

        fun clear(context: Context, messageId: Long) {
            backoff(context).clear(messageId.toString())
        }

        fun pending(context: Context): Set<Long> =
            backoff(context).keys().mapNotNull(String::toLongOrNull).toSet()
    }
}
