/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import javax.inject.Inject
import org.prauga.messages.interactor.ReceiveSms
import timber.log.Timber

class ReceiveSmsWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    companion object {
        const val INPUT_DATA_KEY_MESSAGE_ID = "messageId"
    }

    @Inject lateinit var receiveSms: ReceiveSms

    override fun doWork(): Result {
        Timber.v("started")

        val messageId = inputData.getLong(INPUT_DATA_KEY_MESSAGE_ID, -1)
        if (messageId == -1L) {
            Timber.v("failed. message id was -1")
            return Result.failure(inputData)
        }

        receiveSms.buildObservable(messageId).blockingSubscribe({}, Timber::w)

        Timber.v("finished")

        return Result.success()
    }
}
