/*
 * SPDX-FileCopyrightText: 2020 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.interactor.MarkBlocked
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.Preferences
import timber.log.Timber

class BlockThreadReceiver : BroadcastReceiver() {

    @Inject lateinit var blockingClient: BlockingClient
    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var markBlocked: MarkBlocked
    @Inject lateinit var prefs: Preferences

    @SuppressLint("CheckResult")
    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val threadId = intent.getLongExtra("threadId", 0)

        blockingClient
            .block(
                conversationRepo.getConversation(threadId)?.recipients?.map { it.address }
                    ?: listOf()
            )
            .subscribeOn(Schedulers.io())
            .andThen(
                markBlocked.buildObservable(
                    MarkBlocked.Params(listOf(threadId), prefs.blockingManager.get(), null)
                )
            )
            .subscribe(
                { goAsync().finish() },
                { error ->
                    Timber.e("BlockThreadReceiver", "blocking failed")
                    goAsync().finish()
                },
            )
    }
}
