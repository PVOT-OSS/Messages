/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import android.net.Uri
import android.provider.Telephony.Mms
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

        // The provider gives a deleted row's id to the next one, so make sure it's still the
        // same MMS, and not one that came in since, like from someone blocked
        val location = inputData.getString(KEY_LOCATION)
        if (location == null || location != locationOf(applicationContext, uri)) {
            Timber.v("$uri isn't the MMS to download anymore")
            clear(applicationContext, uri)
            return Result.success()
        }

        Timber.v("Retrying MMS download for $uri")
        messageRepo.downloadMms(uri, subId, automatic = true)
        return Result.success()
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_SUB_ID = "subId"
        private const val KEY_LOCATION = "location"

        private fun name(notificationUri: Uri) = "mms-download-retry:$notificationUri"

        private fun locationOf(context: Context, notificationUri: Uri): String? =
            try {
                context.contentResolver
                    .query(notificationUri, arrayOf(Mms.CONTENT_LOCATION), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
            } catch (e: Exception) {
                Timber.w(e, "Unable to read the location of $notificationUri")
                null
            }

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
                    .setInputData(
                        workDataOf(
                            KEY_URI to key,
                            KEY_SUB_ID to subId,
                            KEY_LOCATION to locationOf(context, notificationUri),
                        )
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(name(notificationUri), ExistingWorkPolicy.REPLACE, request)
        }

        fun clear(context: Context, notificationUri: Uri) {
            backoff(context).clear(notificationUri.toString())
            WorkManager.getInstance(context).cancelUniqueWork(name(notificationUri))
        }
    }
}
