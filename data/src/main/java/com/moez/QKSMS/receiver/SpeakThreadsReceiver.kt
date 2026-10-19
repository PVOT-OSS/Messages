/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.SpeakThreads
import org.prauga.messages.repository.ConversationRepository

class SpeakThreadsReceiver : BroadcastReceiver() {

    @Inject lateinit var speakThread: SpeakThreads
    @Inject lateinit var conversationRepo: ConversationRepository

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val pendingResult = goAsync()
        val threadId = intent.getLongExtra("threadId", 0)

        val threads =
            when {
                (threadId == -1L) -> conversationRepo.getUnseenIds()
                (threadId == -2L) -> conversationRepo.getUnreadIds()
                else -> listOf(threadId)
            }

        speakThread.execute(threads) { pendingResult.finish() }
    }
}
