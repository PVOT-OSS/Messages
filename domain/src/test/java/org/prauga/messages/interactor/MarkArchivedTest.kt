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

class MarkArchivedTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var markRead: MarkRead
    private lateinit var markArchived: MarkArchived

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        markRead = MarkRead(messageRepo, notificationManager, updateBadge)
        markArchived = MarkArchived(conversationRepo, markRead)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksArchived() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).markArchived(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllArchived() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markArchived(100L, 200L, 300L)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenAlsoMarksRead() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllRead() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesNotifications() {
        val threadIds = listOf(100L, 200L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
        verify(notificationManager).update(200L)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesBadge() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksArchived() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markArchived(0L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markArchived(Long.MAX_VALUE)
    }

    @Test
    fun givenManyThreadIds_whenBuildObservable_thenMarksAllArchived() {
        val threadIds = (1L..5L).toList()
        val testSubscriber = TestSubscriber<Any>()

        markArchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markArchived(1L, 2L, 3L, 4L, 5L)
        verify(messageRepo).markRead(threadIds)
    }
}
