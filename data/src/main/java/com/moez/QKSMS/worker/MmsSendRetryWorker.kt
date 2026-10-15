/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import org.prauga.messages.interactor.RetrySending
import timber.log.Timber

class MmsSendRetryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    lateinit var retrySending: RetrySending

    override fun doWork(): Result {
        val messageId =
            inputData.getLong(KEY_MESSAGE_ID, -1).takeIf { it != -1L } ?: return Result.failure()

        Timber.v("Retrying to send MMS $messageId")
        retrySending.buildObservable(messageId).blockingSubscribe({}, Timber::w)
        return Result.success()
    }

    companion object {
        private const val KEY_MESSAGE_ID = "messageId"

        private fun backoff(context: Context) = MmsRetryBackoff(context, "mms_send_retries")

        fun schedule(context: Context, messageId: Long): Boolean {
            val delay = backoff(context).nextDelay(messageId.toString()) ?: return false

            val request =
                OneTimeWorkRequestBuilder<MmsSendRetryWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("mms-send-retry:$messageId", ExistingWorkPolicy.REPLACE, request)
            return true
        }

        fun clear(context: Context, messageId: Long) {
            backoff(context).clear(messageId.toString())
        }
    }
}
