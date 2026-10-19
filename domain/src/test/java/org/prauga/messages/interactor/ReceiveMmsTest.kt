/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.net.Uri
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
import org.prauga.messages.manager.ActiveConversationManager
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageContentFilterRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.util.Preferences

class ReceiveMmsTest {

    @Mock private lateinit var activeConversationManager: ActiveConversationManager

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var blockingClient: BlockingClient

    @Mock private lateinit var prefs: Preferences

    @Mock private lateinit var syncManager: SyncRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    @Mock private lateinit var filterRepo: MessageContentFilterRepository

    @Mock private lateinit var contactsRepo: ContactRepository

    @Mock private lateinit var message: Message

    @Mock private lateinit var conversation: Conversation

    @Mock private lateinit var dropPref: Preference<Boolean>

    @Mock private lateinit var blockingManagerPref: Preference<Int>

    @Mock private lateinit var uri: Uri

    private lateinit var receiveMms: ReceiveMms

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        `when`(prefs.drop).thenReturn(dropPref)
        `when`(prefs.blockingManager).thenReturn(blockingManagerPref)
        `when`(dropPref.get()).thenReturn(false)
        `when`(blockingManagerPref.get()).thenReturn(0)
        receiveMms =
            ReceiveMms(
                activeConversationManager,
                conversationRepo,
                blockingClient,
                prefs,
                syncManager,
                messageRepo,
                notificationManager,
                updateBadge,
                filterRepo,
                contactsRepo,
            )
    }

    @Test
    fun givenUri_whenSyncFails_thenDoesNothing() {
        `when`(syncManager.syncMessage(uri)).thenReturn(null)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenActiveConversation_whenBuildObservable_thenMarksRead() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(threadId)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(listOf(threadId))
    }

    @Test
    fun givenNotActiveConversation_whenBuildObservable_thenDoesNotMarkRead() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(999L) // Different
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenBlockedAndDropEnabled_whenBuildObservable_thenDeletesMessage() {
        val threadId = 100L
        val messageId = 1L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Block("spam")))
        `when`(dropPref.get()).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(listOf(messageId))
    }

    @Test
    fun givenBlockedAndDropDisabled_whenBuildObservable_thenMarksBlocked() {
        val threadId = 100L
        val messageId = 1L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Block("spam")))
        `when`(dropPref.get()).thenReturn(false)
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(listOf(threadId))
        verify(conversationRepo).markBlocked(listOf(threadId), 0, "spam")
    }

    @Test
    fun givenUnblockAction_whenBuildObservable_thenMarksUnblocked() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.Unblock))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(threadId)
    }

    @Test
    fun givenContentFilterBlocked_whenBuildObservable_thenDeletesMessage() {
        val threadId = 100L
        val messageId = 1L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(messageId)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("spam content")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("spam content", "+1234567890", contactsRepo)).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(listOf(messageId))
    }

    @Test
    fun givenNotBlocked_whenBuildObservable_thenUpdatesConversation() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(threadId)
    }

    @Test
    fun givenBlockedConversation_whenBuildObservable_thenDoesNotNotify() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(true)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenArchivedConversation_whenBuildObservable_thenUnarchives() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(true)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(threadId)
    }

    @Test
    fun givenValidMms_whenBuildObservable_thenUpdatesNotification() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(threadId)
    }

    @Test
    fun givenValidMms_whenBuildObservable_thenUpdatesBadge() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.id).thenReturn(1L)
        `when`(message.threadId).thenReturn(threadId)
        `when`(message.address).thenReturn("+1234567890")
        `when`(message.getText()).thenReturn("Hello")
        `when`(activeConversationManager.getActiveConversation()).thenReturn(null)
        `when`(blockingClient.shouldBlock("+1234567890"))
            .thenReturn(Single.just(BlockingClient.Action.DoNothing))
        `when`(filterRepo.isBlocked("Hello", "+1234567890", contactsRepo)).thenReturn(false)
        `when`(conversationRepo.getOrCreateConversation(threadId)).thenReturn(conversation)
        `when`(conversation.blocked).thenReturn(false)
        `when`(conversation.archived).thenReturn(false)
        `when`(conversation.id).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        receiveMms.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }
}
