/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
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
import org.prauga.messages.repository.MessageRepository

class MarkReadTest {

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var markRead: MarkRead

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        markRead = MarkRead(messageRepo, notificationManager, updateBadge)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksRead() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenUpdatesNotification() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllRead() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenUpdatesAllNotifications() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(notificationManager).update(100L)
        verify(notificationManager).update(200L)
        verify(notificationManager).update(300L)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesBadge() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksRead() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
        verify(notificationManager).update(0L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
    }

    @Test
    fun givenDuplicateThreadIds_whenBuildObservable_thenUpdatesNotificationsTwice() {
        val threadIds = listOf(100L, 100L)
        val testSubscriber = TestSubscriber<Any>()

        markRead.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markRead(threadIds)
        verify(notificationManager, org.mockito.Mockito.times(2)).update(100L)
    }
}
