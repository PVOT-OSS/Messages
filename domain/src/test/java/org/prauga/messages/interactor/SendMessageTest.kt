/*
 * SPDX-FileCopyrightText: 2025-2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Conversation
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class SendMessageTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var updateBadge: UpdateBadge

    @Mock private lateinit var shortcutManager: ShortcutManager

    @Mock private lateinit var conversation: Conversation

    @Mock private lateinit var attachment: Attachment

    private lateinit var sendMessage: SendMessage

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        sendMessage = SendMessage(conversationRepo, messageRepo, updateBadge, shortcutManager)
    }

    @Test
    fun givenValidParams_whenBuildObservable_thenSendsMessage() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Hello World",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo)
            .sendMessage(1, 100L, listOf("+1234567890"), "Hello World", emptyList(), 0)
        verify(conversationRepo).updateConversations(100L)
        verify(conversationRepo).markUnarchived(100L)
        verify(shortcutManager).reportShortcutUsed(100L)
    }

    @Test
    fun givenEmptyAddresses_whenBuildObservable_thenDoesNotSendMessage() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = emptyList(),
                body = "Hello World",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenCreatesConversation() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 0L,
                addresses = listOf("+1234567890"),
                body = "Hello World",
            )
        `when`(conversation.id).thenReturn(200L)
        `when`(conversationRepo.getOrCreateConversation(listOf("+1234567890")))
            .thenReturn(conversation)
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).getOrCreateConversation(listOf("+1234567890"))
        verify(messageRepo)
            .sendMessage(1, 200L, listOf("+1234567890"), "Hello World", emptyList(), 0)
    }

    @Test
    fun givenZeroThreadIdAndNoConversationCreated_whenBuildObservable_thenDoesNotSendMessage() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 0L,
                addresses = listOf("+1234567890"),
                body = "Hello World",
            )
        `when`(conversationRepo.getOrCreateConversation(listOf("+1234567890"))).thenReturn(null)
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenMultipleAddresses_whenBuildObservable_thenSendsGroupMessage() {
        val addresses = listOf("+1234567890", "+0987654321", "+1122334455")
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = addresses,
                body = "Group message",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).sendMessage(1, 100L, addresses, "Group message", emptyList(), 0)
    }

    @Test
    fun givenAttachments_whenBuildObservable_thenSendsWithAttachments() {
        val attachments = listOf(attachment)
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Message with attachment",
                attachments = attachments,
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo)
            .sendMessage(1, 100L, listOf("+1234567890"), "Message with attachment", attachments, 0)
        verifyNoInteractions(attachment)
    }

    @Test
    fun givenDelay_whenBuildObservable_thenSendsWithDelay() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Delayed message",
                delay = 5000,
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo)
            .sendMessage(1, 100L, listOf("+1234567890"), "Delayed message", emptyList(), 5000)
    }

    @Test
    fun givenValidParams_whenBuildObservable_thenUpdatesBadge() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Hello World",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyBody_whenBuildObservable_thenSendsMessage() {
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).sendMessage(1, 100L, listOf("+1234567890"), "", emptyList(), 0)
    }

    @Test
    fun givenDifferentSubId_whenBuildObservable_thenUsesCorrectSubId() {
        val params =
            SendMessage.Params(
                subId = 2,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Test",
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).sendMessage(2, 100L, listOf("+1234567890"), "Test", emptyList(), 0)
    }

    @Test
    fun givenMultipleAttachments_whenBuildObservable_thenLeavesCleanupToCaller() {
        val attachment1 = org.mockito.Mockito.mock(Attachment::class.java)
        val attachment2 = org.mockito.Mockito.mock(Attachment::class.java)
        val attachment3 = org.mockito.Mockito.mock(Attachment::class.java)
        val attachments = listOf(attachment1, attachment2, attachment3)
        val params =
            SendMessage.Params(
                subId = 1,
                threadId = 100L,
                addresses = listOf("+1234567890"),
                body = "Multiple attachments",
                attachments = attachments,
            )
        val testSubscriber = TestSubscriber<Any>()

        sendMessage.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verifyNoInteractions(attachment1, attachment2, attachment3)
    }
}
