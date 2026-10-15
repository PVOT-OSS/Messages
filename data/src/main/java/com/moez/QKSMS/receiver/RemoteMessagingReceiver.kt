/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.compat.SubscriptionManagerCompat
import org.prauga.messages.interactor.MarkRead
import org.prauga.messages.interactor.SendMessage
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class RemoteMessagingReceiver : BroadcastReceiver() {

    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var markRead: MarkRead
    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var sendMessage: SendMessage
    @Inject lateinit var subscriptionManager: SubscriptionManagerCompat

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val bundle = intent.extras ?: return

        val threadId = bundle.getLong("threadId")
        val body = remoteInput.getCharSequence("body").toString()
        markRead.execute(listOf(threadId))

        val lastMessage = messageRepo.getMessages(threadId).lastOrNull()
        val subId =
            subscriptionManager.activeSubscriptionInfoList
                .firstOrNull { it.subscriptionId == lastMessage?.subId }
                ?.subscriptionId ?: -1
        val addresses =
            conversationRepo.getConversation(threadId)?.recipients?.map { it.address } ?: return

        val pendingRepository = goAsync()
        sendMessage.execute(SendMessage.Params(subId, threadId, addresses, body)) {
            pendingRepository.finish()
        }
    }
}
