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
import org.prauga.messages.repository.MessageRepository

class MarkBlockedTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var markRead: MarkRead
    private lateinit var markBlocked: MarkBlocked

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        markRead = MarkRead(messageRepo, notificationManager, updateBadge)
        markBlocked = MarkBlocked(conversationRepo, markRead)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksBlocked() {
        val params =
            MarkBlocked.Params(threadIds = listOf(100L), blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).markBlocked(listOf(100L), 0, null)
    }

    @Test
    fun givenBlockReason_whenBuildObservable_thenIncludesReason() {
        val params =
            MarkBlocked.Params(threadIds = listOf(100L), blockingClient = 1, blockReason = "spam")
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markBlocked(listOf(100L), 1, "spam")
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllBlocked() {
        val threadIds = listOf(100L, 200L, 300L)
        val params =
            MarkBlocked.Params(threadIds = threadIds, blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markBlocked(threadIds, 0, null)
    }

    @Test
    fun givenParams_whenBuildObservable_thenAlsoMarksRead() {
        val threadIds = listOf(100L)
        val params =
            MarkBlocked.Params(threadIds = threadIds, blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenParams_whenBuildObservable_thenUpdatesNotifications() {
        val threadIds = listOf(100L, 200L)
        val params =
            MarkBlocked.Params(threadIds = threadIds, blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
        verify(notificationManager).update(200L)
    }

    @Test
    fun givenParams_whenBuildObservable_thenUpdatesBadge() {
        val params =
            MarkBlocked.Params(threadIds = listOf(100L), blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyThreadIds_whenBuildObservable_thenStillCompletes() {
        val params =
            MarkBlocked.Params(threadIds = emptyList(), blockingClient = 0, blockReason = null)
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenDifferentBlockingClient_whenBuildObservable_thenUsesCorrectClient() {
        val params =
            MarkBlocked.Params(
                threadIds = listOf(100L),
                blockingClient = 5,
                blockReason = "malware",
            )
        val testSubscriber = TestSubscriber<Any>()

        markBlocked.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markBlocked(listOf(100L), 5, "malware")
    }
}
