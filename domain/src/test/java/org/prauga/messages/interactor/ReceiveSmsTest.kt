/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageContentFilterRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.Preferences

class ReceiveSmsTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var blockingClient: BlockingClient

    @Mock private lateinit var prefs: Preferences

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    @Mock private lateinit var shortcutManager: ShortcutManager

    @Mock private lateinit var filterRepo: MessageContentFilterRepository

    @Mock private lateinit var contactsRepo: ContactRepository

    @Mock private lateinit var message: Message

    @Mock private lateinit var conversation: Conversation

    @Mock private lateinit var dropPref: Preference<Boolean>

    @Mock private lateinit var blockingManagerPref: Preference<Int>

    private lateinit var receiveSms: ReceiveSms

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        `when`(prefs.drop).thenReturn(dropPref)
        `when`(prefs.blockingManager).thenReturn(blockingManagerPref)
        `when`(dropPref.get()).thenReturn(false)
        `when`(blockingManagerPref.get()).thenReturn(0)
        receiveSms =
            ReceiveSms(
                conversationRepo,
                blockingClient,
                prefs,
                messageRepo,
                notificationManager,
                updateBadge,
                shortcutManager,
                filterRepo,
                contactsRepo,
            )
    }

    @Test
    fun givenValidMessageId_whenMessageNotFound_thenDoesNothing() {
        val messageId = 100L
        `when`(messageRepo.getMessage(messageId)).thenReturn(null)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenBlockedAddressAndDropEnabled_whenBuildObservable_thenDeletesMessage() {
        val messageId = 100L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Block("spam")))
        `when`(dropPref.get()).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(listOf(messageId))
    }

    @Test
    fun givenBlockedAddressAndDropDisabled_whenBuildObservable_thenMarksBlocked() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Block("spam")))
        `when`(dropPref.get()).thenReturn(false)
        `when`(message.getText()).thenReturn("")
        `when`(filterRepo.isBlocked("", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(listOf(threadId))
        verify(conversationRepo).markBlocked(listOf(threadId), 0, "spam")
    }

    @Test
    fun givenUnblockAction_whenBuildObservable_thenMarksUnblocked() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Unblock))
        `when`(message.getText()).thenReturn("")
        `when`(filterRepo.isBlocked("", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(threadId)
    }

    @Test
    fun givenContentFilterBlocked_whenBuildObservable_thenDeletesMessage() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("spam content")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("spam content", "+1234567890", contactsRepo)).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(listOf(messageId))
    }

    @Test
    fun givenNotBlockedAndNotFiltered_whenBuildObservable_thenUpdatesConversation() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(threadId)
    }

    @Test
    fun givenBlockedConversation_whenBuildObservable_thenDoesNotNotify() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenArchivedConversation_whenBuildObservable_thenUnarchives() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(true)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(threadId)
    }

    @Test
    fun givenValidMessage_whenBuildObservable_thenUpdatesNotification() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(threadId)
    }

    @Test
    fun givenValidMessage_whenBuildObservable_thenUpdatesShortcuts() {
        val messageId = 100L
        val threadId = 200L
        `when`(messageRepo.getMessage(messageId)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveSms.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(shortcutManager).updateShortcuts()
        verify(shortcutManager).reportShortcutUsed(threadId)
    }
}
