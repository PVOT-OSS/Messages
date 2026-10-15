/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.net.Uri
import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.manager.ActiveConversationManager
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageContentFilterRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.util.Preferences
import timber.log.Timber

class ReceiveMms
@Inject
constructor(
    private val activeConversationManager: ActiveConversationManager,
    private val conversationRepo: ConversationRepository,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences,
    private val syncManager: SyncRepository,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
    private val filterRepo: MessageContentFilterRepository,
    private val contactsRepo: ContactRepository,
) : Interactor<Uri>() {

    override fun buildObservable(params: Uri): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull(syncManager::syncMessage) // Sync the message
            .doOnNext { message ->
                // TODO: Ideally this is done when we're saving the MMS to ContentResolver
                // This change can be made once we move the MMS storing code to the Data module
                if (activeConversationManager.getActiveConversation() == message.threadId) {
                    messageRepo.markRead(listOf(message.threadId))
                }
            }
            .mapNotNull { message ->
                // Because we use the smsmms library for receiving and storing MMS, we'll need
                // to check if it should be blocked after we've pulled it into realm. If it
                // turns out that it should be dropped, then delete it
                // TODO Don't store blocked messages in the first place
                val action = blockingClient.shouldBlock(message.address).blockingGet()
                val shouldDrop = prefs.drop.get()
                Timber.v("block=$action, drop=$shouldDrop")

                if (action is BlockingClient.Action.Block && shouldDrop) {
                    messageRepo.deleteMessages(listOf(message.id))
                    return@mapNotNull null
                }

                when (action) {
                    is BlockingClient.Action.Block -> {
                        messageRepo.markRead(listOf(message.threadId))
                        conversationRepo.markBlocked(
                            listOf(message.threadId),
                            prefs.blockingManager.get(),
                            action.reason,
                        )
                    }
                    is BlockingClient.Action.Unblock ->
                        conversationRepo.markUnblocked(message.threadId)
                    else -> Unit
                }

                if (filterRepo.isBlocked(message.getText(), message.address, contactsRepo)) {
                    Timber.v("message dropped based on content filters")
                    messageRepo.deleteMessages(listOf(message.id))
                    return@mapNotNull null
                }

                message
            }
            .doOnNext { message ->
                conversationRepo.updateConversations(message.threadId) // Update the conversation
            }
            .mapNotNull { message ->
                conversationRepo.getOrCreateConversation(
                    message.threadId
                ) // Map message to conversation
            }
            .filter { conversation ->
                !conversation.blocked
            } // Don't notify for blocked conversations
            .doOnNext { conversation ->
                // Unarchive conversation if necessary
                if (conversation.archived) conversationRepo.markUnarchived(conversation.id)
            }
            .map { conversation ->
                conversation.id
            } // Map to the id because [delay] will put us on the wrong thread
            .doOnNext(notificationManager::update) // Update the notification
            .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }
}
