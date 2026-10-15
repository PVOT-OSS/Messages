/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
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

class DeleteConversationsTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var deleteConversations: DeleteConversations

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        deleteConversations =
            DeleteConversations(conversationRepo, notificationManager, updateBadge)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenDeletesConversation() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).deleteConversations(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenDeletesAllConversations() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).deleteConversations(100L, 200L, 300L)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenUpdatesNotification() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenUpdatesAllNotifications() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
        verify(notificationManager).update(200L)
        verify(notificationManager).update(300L)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesBadge() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenDeletesZeroThread() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).deleteConversations(0L)
        verify(notificationManager).update(0L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).deleteConversations(Long.MAX_VALUE)
    }

    @Test
    fun givenManyThreadIds_whenBuildObservable_thenDeletesAll() {
        val threadIds = (1L..10L).toList()
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).deleteConversations(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
    }

    @Test
    fun givenNegativeThreadId_whenBuildObservable_thenStillDeletes() {
        val threadIds = listOf(-1L)
        val testSubscriber = TestSubscriber<Any>()

        deleteConversations.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).deleteConversations(-1L)
    }
}
