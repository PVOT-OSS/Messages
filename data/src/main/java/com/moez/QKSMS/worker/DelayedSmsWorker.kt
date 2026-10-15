/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import org.prauga.messages.receiver.SendSmsReceiver

class DelayedSmsWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val messageId =
            inputData.getLong(KEY_MESSAGE_ID, -1).takeIf { it != -1L } ?: return Result.failure()

        applicationContext.sendBroadcast(
            Intent(applicationContext, SendSmsReceiver::class.java).putExtra("id", messageId)
        )
        return Result.success()
    }

    companion object {
        private const val KEY_MESSAGE_ID = "messageId"

        private fun name(messageId: Long) = "delayed-sms:$messageId"

        fun schedule(context: Context, messageId: Long, delay: Long) {
            val request =
                OneTimeWorkRequestBuilder<DelayedSmsWorker>()
                    .setInitialDelay(delay.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(name(messageId), ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, messageId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(name(messageId))
        }
    }
}
