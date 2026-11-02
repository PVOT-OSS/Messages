/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.model.MessageSelection
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class DeleteMessages
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
) : Interactor<DeleteMessages.Params>() {

    /**
     * With [unseenUntil], deletes the unread, unseen messages up to that date instead of
     * [messageIds], and with [selection], those selected
     */
    data class Params(
        val messageIds: List<Long>,
        val threadId: Long,
        val unseenUntil: Long? = null,
        val selection: MessageSelection? = null,
    )

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext {
                when {
                    params.selection != null -> messageRepo.deleteMessages(params.selection)
                    params.unseenUntil != null ->
                        messageRepo.deleteUnreadUnseenMessages(params.threadId, params.unseenUntil)
                    else -> messageRepo.deleteMessages(params.messageIds)
                }
            } // Delete the messages
            .doOnNext {
                conversationRepo.updateConversations(params.threadId)
            } // Update the conversation
            .doOnNext { notificationManager.update(params.threadId) }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
