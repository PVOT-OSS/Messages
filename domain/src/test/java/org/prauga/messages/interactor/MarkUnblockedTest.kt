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
import org.prauga.messages.repository.ConversationRepository

class MarkUnblockedTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    private lateinit var markUnblocked: MarkUnblocked

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markUnblocked = MarkUnblocked(conversationRepo)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksUnblocked() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).markUnblocked(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllUnblocked() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(100L, 200L, 300L)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksUnblocked() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(0L)
    }

    @Test
    fun givenNegativeThreadId_whenBuildObservable_thenStillCalls() {
        val threadIds = listOf(-1L)
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(-1L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(Long.MAX_VALUE)
    }

    @Test
    fun givenManyThreadIds_whenBuildObservable_thenMarksAllUnblocked() {
        val threadIds = (1L..10L).toList()
        val testSubscriber = TestSubscriber<Any>()

        markUnblocked.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnblocked(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
    }
}
