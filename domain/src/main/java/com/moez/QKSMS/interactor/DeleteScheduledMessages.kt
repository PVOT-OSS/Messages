/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.annotation.SuppressLint
import android.content.Context
import io.reactivex.Flowable
import java.io.File
import javax.inject.Inject
import org.prauga.messages.repository.ScheduledMessageRepository
import timber.log.Timber

class DeleteScheduledMessages
@Inject
constructor(
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val context: Context,
) : Interactor<List<Long>>() {

    @SuppressLint("Range")
    override fun buildObservable(scheduledMessageIds: List<Long>): Flowable<*> {
        return Flowable.just(scheduledMessageIds).map {
            try {
                // for each message id to delete
                it.forEach {
                    // recursively delete scheduled message top level dir
                    val topDir = File(context.filesDir, "scheduled-${it}")
                    topDir.exists() && topDir.deleteRecursively()
                }
            } catch (e: Exception) {
                Timber.e("Unable to delete scheduled messages.")
            }

            // delete the db entries
            scheduledMessageRepo.deleteScheduledMessages(it)
        }
    }
}
