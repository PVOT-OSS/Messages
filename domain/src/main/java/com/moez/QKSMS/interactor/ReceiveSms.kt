/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageContentFilterRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.Preferences
import timber.log.Timber

class ReceiveSms
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager,
    private val filterRepo: MessageContentFilterRepository,
    private val contactsRepo: ContactRepository,
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull { messageRepo.getMessage(it) }
            .mapNotNull {
                val action = blockingClient.shouldBlock(it.address).blockingGet()

                when {
                    ((action is BlockingClient.Action.Block) && prefs.drop.get()) -> {
                        // blocked and 'drop blocked.' remove from db and don't continue
                        Timber.v("address is blocked and drop blocked is on. dropped")
                        messageRepo.deleteMessages(listOf(it.id))
                        return@mapNotNull null
                    }
                    action is BlockingClient.Action.Block -> {
                        // blocked
                        Timber.v("address is blocked")
                        messageRepo.markRead(listOf(it.threadId))
                        conversationRepo.markBlocked(
                            listOf(it.threadId),
                            prefs.blockingManager.get(),
                            action.reason,
                        )
                    }
                    action is BlockingClient.Action.Unblock -> {
                        // unblock
                        Timber.v("unblock conversation if blocked")
                        conversationRepo.markUnblocked(it.threadId)
                    }
                }

                if (filterRepo.isBlocked(it.getText(), it.address, contactsRepo)) {
                    Timber.v("message dropped based on content filters")
                    messageRepo.deleteMessages(listOf(it.id))
                    return@mapNotNull null
                }

                // update and fetch conversation
                conversationRepo.updateConversations(it.threadId)
                conversationRepo.getOrCreateConversation(it.threadId)
            }
            .mapNotNull {
                // don't notify (continue) for blocked conversations
                if (it.blocked) {
                    Timber.v("no notifications for blocked")
                    return@mapNotNull null
                }

                // unarchive conversation if necessary
                if (it.archived) {
                    Timber.v("conversation unarchived")
                    conversationRepo.markUnarchived(it.id)
                }

                // update/create notification
                Timber.v("update/create notification")
                notificationManager.update(it.id)

                // update shortcuts
                Timber.v("update shortcuts")
                shortcutManager.updateShortcuts()
                shortcutManager.reportShortcutUsed(it.id)

                // update the badge and widget
                Timber.v("update badge and widget")
                updateBadge.buildObservable(Unit)
            }
    }
}
