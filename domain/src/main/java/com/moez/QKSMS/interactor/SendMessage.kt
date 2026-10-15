/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.model.Attachment
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class SendMessage
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager,
) : Interactor<SendMessage.Params>() {

    data class Params(
        val subId: Int,
        val threadId: Long,
        val addresses: List<String>,
        val body: String,
        val attachments: List<Attachment> = listOf(),
        val delay: Int = 0,
        val applySignature: Boolean = true,
    )

    override fun buildObservable(params: Params): Flowable<*> =
        Flowable.just(Unit)
            .filter { params.addresses.isNotEmpty() }
            .doOnNext {
                // If a threadId isn't provided, try to obtain one
                val threadId =
                    when (params.threadId) {
                        0L -> conversationRepo.getOrCreateConversation(params.addresses)?.id ?: 0
                        else -> params.threadId
                    }

                // if unable to find or create a conversation (and/or underlying threadId), fail
                if (threadId == 0L) return@doOnNext

                params.apply {
                    messageRepo.sendMessage(
                        subId,
                        threadId,
                        addresses,
                        body,
                        attachments,
                        delay,
                        applySignature,
                    )
                }

                conversationRepo.updateConversations(threadId)

                conversationRepo.markUnarchived(threadId)

                shortcutManager.reportShortcutUsed(threadId)
            }
            .flatMap { updateBadge.buildObservable(Unit) } // Update the widget
}
