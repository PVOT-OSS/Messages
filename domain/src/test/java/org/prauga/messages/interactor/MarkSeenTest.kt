/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.MessageRepository

class MarkSeenTest {

    @Mock private lateinit var messageRepo: MessageRepository

    private lateinit var markSeen: MarkSeen

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markSeen = MarkSeen(messageRepo)
    }

    @Test
    fun givenThreadId_whenBuildObservable_thenMarksSeen() {
        val threadId = 100L
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo).markSeen(threadId)
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksSeen() {
        val threadId = 0L
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markSeen(0L)
    }

    @Test
    fun givenNegativeThreadId_whenBuildObservable_thenStillCalls() {
        val threadId = -1L
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markSeen(-1L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadId = Long.MAX_VALUE
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markSeen(Long.MAX_VALUE)
    }

    @Test
    fun givenMinValueThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadId = Long.MIN_VALUE
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).markSeen(Long.MIN_VALUE)
    }

    @Test
    fun givenThreadId_whenBuildObservable_thenEmitsUnit() {
        val threadId = 100L
        val testSubscriber = TestSubscriber<Unit>()

        markSeen.buildObservable(threadId).subscribe(testSubscriber)

        testSubscriber.assertValueCount(1)
        testSubscriber.assertValue(Unit)
    }
}
