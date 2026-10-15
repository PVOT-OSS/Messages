/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.worker

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import org.prauga.messages.repository.MessageRepository
import timber.log.Timber

class MmsDownloadRetryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    lateinit var messageRepo: MessageRepository

    override fun doWork(): Result {
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return Result.failure()
        val subId = inputData.getInt(KEY_SUB_ID, -1)

        Timber.v("Retrying MMS download for $uri")
        messageRepo.downloadMms(uri, subId, automatic = true)
        return Result.success()
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_SUB_ID = "subId"

        private fun backoff(context: Context) =
            MmsRetryBackoff(context, "mms_download_retries", MmsRetryBackoff.DOWNLOAD_WINDOW)

        fun schedule(context: Context, notificationUri: Uri, subId: Int) {
            val key = notificationUri.toString()
            val delay = backoff(context).nextDelay(key)
            if (delay == null) {
                Timber.v("Giving up on retrying $notificationUri")
                return
            }

            val request =
                OneTimeWorkRequestBuilder<MmsDownloadRetryWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .setInputData(workDataOf(KEY_URI to key, KEY_SUB_ID to subId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("mms-download-retry:$key", ExistingWorkPolicy.REPLACE, request)
        }

        fun clear(context: Context, notificationUri: Uri) {
            backoff(context).clear(notificationUri.toString())
        }
    }
}
