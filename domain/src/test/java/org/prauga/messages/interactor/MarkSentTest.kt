/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.MessageRepository

class MarkSentTest {

    @Mock private lateinit var messageRepository: MessageRepository

    private lateinit var markSent: MarkSent

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markSent = MarkSent(messageRepository)
    }

    @Test
    fun givenMessageId_whenBuildObservable_thenCallsMarkSentOnRepository() {
        val messageId = 123L
        val testSubscriber = TestSubscriber<Unit>()

        markSent.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markSent(messageId)
    }

    @Test
    fun givenZeroMessageId_whenBuildObservable_thenStillCallsRepository() {
        val messageId = 0L
        val testSubscriber = TestSubscriber<Unit>()

        markSent.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markSent(messageId)
    }

    @Test
    fun givenNegativeMessageId_whenBuildObservable_thenStillCallsRepository() {
        val messageId = -1L
        val testSubscriber = TestSubscriber<Unit>()

        markSent.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markSent(messageId)
    }

    @Test
    fun givenLargeMessageId_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = Long.MAX_VALUE
        val testSubscriber = TestSubscriber<Unit>()

        markSent.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markSent(messageId)
    }
}
