/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.ConversationRepository

class MarkUnarchivedTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    private lateinit var markUnarchived: MarkUnarchived

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markUnarchived = MarkUnarchived(conversationRepo)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksUnarchived() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).markUnarchived(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllUnarchived() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(100L, 200L, 300L)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksUnarchived() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(0L)
    }

    @Test
    fun givenNegativeThreadId_whenBuildObservable_thenStillCalls() {
        val threadIds = listOf(-1L)
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(-1L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(Long.MAX_VALUE)
    }

    @Test
    fun givenManyThreadIds_whenBuildObservable_thenMarksAllUnarchived() {
        val threadIds = (1L..10L).toList()
        val testSubscriber = TestSubscriber<Any>()

        markUnarchived.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markUnarchived(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
    }
}
