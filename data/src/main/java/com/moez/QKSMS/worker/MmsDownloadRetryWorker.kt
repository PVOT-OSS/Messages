/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.worker

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.random.Random
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
        private const val PREFS = "mms_download_retries"

        private val MIN_DELAY = TimeUnit.SECONDS.toMillis(5)
        private val MAX_DELAY = TimeUnit.MINUTES.toMillis(20)
        private val RETRY_WINDOW = TimeUnit.HOURS.toMillis(12)

        fun schedule(context: Context, notificationUri: Uri, subId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = notificationUri.toString()
            val now = System.currentTimeMillis()
            val (attempt, firstFailure) =
                prefs.getString(key, null)
                    ?.split(',')
                    ?.mapNotNull(String::toLongOrNull)
                    ?.takeIf { it.size == 2 }
                    ?.let { (attempt, firstFailure) -> attempt.toInt() to firstFailure }
                    ?: (0 to now)

            if (now - firstFailure > RETRY_WINDOW) {
                Timber.v("Giving up on retrying $notificationUri")
                clear(context, notificationUri)
                return
            }

            prefs.edit { putString(key, "${attempt + 1},$firstFailure") }

            val request =
                OneTimeWorkRequestBuilder<MmsDownloadRetryWorker>()
                    .setInitialDelay(retryDelay(attempt), TimeUnit.MILLISECONDS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .setInputData(workDataOf(KEY_URI to key, KEY_SUB_ID to subId))
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(workName(notificationUri), ExistingWorkPolicy.REPLACE, request)
        }

        fun clear(context: Context, notificationUri: Uri) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                remove(notificationUri.toString())
            }
        }

        fun retryDelay(attempt: Int, random: Random = Random.Default): Long {
            val delay = (MIN_DELAY shl attempt.coerceIn(0, 20)).coerceAtMost(MAX_DELAY)
            return (delay / 2 + random.nextLong(delay / 2 + 1)).coerceAtLeast(MIN_DELAY)
        }

        private fun workName(notificationUri: Uri) = "mms-download-retry:$notificationUri"
    }
}
