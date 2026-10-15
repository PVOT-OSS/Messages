/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import org.prauga.messages.interactor.ReceiveSms
import org.prauga.messages.interactor.RetrySending
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.repository.ScheduledMessageRepository

class InjectionWorkerFactory
@Inject
constructor(
    private val messageRepository: MessageRepository,
    private val receiveSms: ReceiveSms,
    private val retrySending: RetrySending,
    private val scheduledMessageRepository: ScheduledMessageRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val instance =
            Class.forName(workerClassName)
                .asSubclass(Worker::class.java)
                .getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
                .newInstance(appContext, workerParameters)

        when (instance) {
            is HousekeepingWorker -> {
                instance.scheduledMessageRepository = scheduledMessageRepository
            }
            is ReceiveSmsWorker -> {
                instance.receiveSms = receiveSms
            }
            is MmsDownloadRetryWorker -> {
                instance.messageRepo = messageRepository
            }
            is MmsSendRetryWorker -> {
                instance.retrySending = retrySending
            }
            is SmsSendRetryWorker -> {
                instance.messageRepo = messageRepository
                instance.retrySending = retrySending
            }
        }

        return instance
    }
}
