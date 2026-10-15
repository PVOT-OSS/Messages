/*
 * SPDX-FileCopyrightText: 2020 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.Preferences
import timber.log.Timber

class DeleteOldMessages
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val prefs: Preferences,
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> =
        Flowable.fromCallable {
            val maxAge = prefs.autoDelete.get().takeIf { it > 0 } ?: return@fromCallable
            val counts = messageRepo.getOldMessageCounts(maxAge)
            val threadIds = counts.keys.toLongArray()

            Timber.d(
                "Deleting ${counts.values.sum()} old messages from ${threadIds.size} conversations"
            )
            messageRepo.deleteOldMessages(maxAge)
            conversationRepo.updateConversations(*threadIds)
        }
}
