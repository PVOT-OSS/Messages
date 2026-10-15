/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class SendDelayedMessageNow
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
) : Interactor<SendDelayedMessageNow.Params>() {

    data class Params(val messageId: Long, val threadId: Long)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(Unit)
            .doOnNext { messageRepo.sendDelayedSmsNow(params.messageId) }
            .doOnNext { conversationRepo.updateConversations(params.threadId) }
    }
}
