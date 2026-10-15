/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.moez.QKSMS.manager.MediaRecorderManager
import com.moez.QKSMS.util.Constants
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.prauga.messages.repository.ScheduledMessageRepository

class HousekeepingWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    companion object {
        private val WORKER_TAG: String = HousekeepingWorker::class.java.simpleName

        fun register(context: Context) {
            // don't check return value because, well, we can't do much about a failure
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_TAG,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequest.Builder(HousekeepingWorker::class.java, 1, TimeUnit.DAYS)
                        .setConstraints(
                            Constraints.Builder()
                                // idle device constraint helps guarantees quik won't be in use
                                // as files are deleted (primarily for deleting audio recordings)
                                .setRequiresDeviceIdle(true)
                                // good citizens don't use up low batteries
                                .setRequiresBatteryNotLow(true)
                                .build()
                        )
                        .addTag(WORKER_TAG)
                        .build(),
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_TAG)
        }
    }

    @Inject lateinit var scheduledMessageRepository: ScheduledMessageRepository

    override fun doWork(): Result {
        val twoHoursAgo = (System.currentTimeMillis() - (2 * 60 * 60 * 1000))

        removeOrphanedScheduledMessageAttachmentFiles()

        removeOrphanedComposeAudioRecordings(twoHoursAgo)

        removeSavedMessagesTexts(twoHoursAgo)

        return Result.success()
    }

    private fun removeOrphanedScheduledMessageAttachmentFiles() {
        // get list of all scheduled message ids
        val scheduledMessageIds = scheduledMessageRepository.getAllScheduledMessageIdsSnapshot()

        // remove orphaned scheduled message dirs in files dir
        File(applicationContext.filesDir, "")
            // get dirs that match prefix 'scheduled-'
            .listFiles { entry -> entry.isDirectory && entry.name.startsWith("scheduled-") }
            // filter out any dirs that have an associated scheduled message in db
            ?.filterNot { scheduledMessageIds.contains(it.name.substringAfter('-').toLong()) }
            // recursively delete orphan dir
            ?.forEach { it.deleteRecursively() }
    }

    private fun removeOrphanedComposeAudioRecordings(removeOlderThan: Long) =
        // find recording files in cache dir
        applicationContext.cacheDir
            .listFiles { entry ->
                entry.isFile &&
                    entry.name.startsWith(MediaRecorderManager.AUDIO_FILE_PREFIX) &&
                    entry.name.endsWith(MediaRecorderManager.AUDIO_FILE_SUFFIX) &&
                    (entry.lastModified() < removeOlderThan)
            }
            ?.forEach { it.delete() } // delete recording file

    private fun removeSavedMessagesTexts(removeOlderThan: Long) =
        // find saved message text files in cache dir
        applicationContext.cacheDir
            .listFiles { entry ->
                entry.isFile &&
                    entry.name.startsWith(Constants.SAVED_MESSAGE_TEXT_FILE_PREFIX) &&
                    (entry.lastModified() < removeOlderThan)
            }
            ?.forEach { it.delete() } // delete message text file
}
