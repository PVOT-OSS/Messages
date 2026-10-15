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
import org.prauga.messages.repository.MessageRepository

class MarkUnreadTest {

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var markUnread: MarkUnread

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        markUnread = MarkUnread(messageRepo, updateBadge)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksUnread() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllUnread() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesBadge() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksUnread() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenNegativeThreadId_whenBuildObservable_thenStillCalls() {
        val threadIds = listOf(-1L)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }

    @Test
    fun givenManyThreadIds_whenBuildObservable_thenMarksAllUnread() {
        val threadIds = (1L..100L).toList()
        val testSubscriber = TestSubscriber<Any>()

        markUnread.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markUnread(threadIds)
    }
}
