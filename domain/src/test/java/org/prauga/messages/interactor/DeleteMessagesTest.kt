/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class DeleteMessagesTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var deleteMessages: DeleteMessages

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        deleteMessages =
            DeleteMessages(conversationRepo, messageRepo, notificationManager, updateBadge)
    }

    @Test
    fun givenSingleMessageId_whenBuildObservable_thenDeletesMessage() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo).deleteMessages(listOf(1L))
    }

    @Test
    fun givenMultipleMessageIds_whenBuildObservable_thenDeletesAllMessages() {
        val messageIds = listOf(1L, 2L, 3L, 4L, 5L)
        val params = DeleteMessages.Params(messageIds = messageIds, threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(messageIds)
    }

    @Test
    fun givenParams_whenBuildObservable_thenUpdatesConversation() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(100L)
    }

    @Test
    fun givenParams_whenBuildObservable_thenUpdatesNotification() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
    }

    @Test
    fun givenParams_whenBuildObservable_thenUpdatesBadge() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyMessageIds_whenBuildObservable_thenStillCompletes() {
        val params = DeleteMessages.Params(messageIds = emptyList(), threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(emptyList())
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenUpdatesZeroThread() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 0L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(0L)
        verify(notificationManager).update(0L)
    }

    @Test
    fun givenLargeMessageIds_whenBuildObservable_thenHandlesCorrectly() {
        val messageIds = listOf(Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val params = DeleteMessages.Params(messageIds = messageIds, threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(messageIds)
    }

    @Test
    fun givenManyMessages_whenBuildObservable_thenDeletesAll() {
        val messageIds = (1L..100L).toList()
        val params = DeleteMessages.Params(messageIds = messageIds, threadId = 100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteMessages(messageIds)
    }

    @Test
    fun givenDifferentThreadId_whenBuildObservable_thenUsesCorrectThreadId() {
        val params = DeleteMessages.Params(messageIds = listOf(1L), threadId = 999L)
        val testSubscriber = TestSubscriber<Any>()

        deleteMessages.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(999L)
        verify(notificationManager).update(999L)
    }
}
