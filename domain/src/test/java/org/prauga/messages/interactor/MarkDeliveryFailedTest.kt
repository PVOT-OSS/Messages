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

class MarkDeliveryFailedTest {

    @Mock private lateinit var messageRepository: MessageRepository

    private lateinit var markDeliveryFailed: MarkDeliveryFailed

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markDeliveryFailed = MarkDeliveryFailed(messageRepository)
    }

    @Test
    fun givenParams_whenBuildObservable_thenCallsMarkDeliveryFailedOnRepository() {
        val messageId = 123L
        val resultCode = 0 // Activity.RESULT_CANCELED
        val params = MarkDeliveryFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markDeliveryFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markDeliveryFailed(messageId, resultCode)
    }

    @Test
    fun givenZeroMessageId_whenBuildObservable_thenStillCallsRepository() {
        val messageId = 0L
        val resultCode = -1
        val params = MarkDeliveryFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markDeliveryFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markDeliveryFailed(messageId, resultCode)
    }

    @Test
    fun givenNegativeResultCode_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = 456L
        val resultCode = -999
        val params = MarkDeliveryFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markDeliveryFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markDeliveryFailed(messageId, resultCode)
    }

    @Test
    fun givenLargeMessageId_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = Long.MAX_VALUE
        val resultCode = 1
        val params = MarkDeliveryFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markDeliveryFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markDeliveryFailed(messageId, resultCode)
    }

    @Test
    fun givenParamsDataClass_whenCreated_thenContainsCorrectValues() {
        val id = 999L
        val resultCode = -5
        val params = MarkDeliveryFailed.Params(id, resultCode)

        org.junit.Assert.assertEquals(id, params.id)
        org.junit.Assert.assertEquals(resultCode, params.resultCode)
    }

    @Test
    fun givenTwoParamsWithSameValues_whenCompared_thenAreEqual() {
        val params1 = MarkDeliveryFailed.Params(100L, 0)
        val params2 = MarkDeliveryFailed.Params(100L, 0)

        org.junit.Assert.assertEquals(params1, params2)
        org.junit.Assert.assertEquals(params1.hashCode(), params2.hashCode())
    }

    @Test
    fun givenTwoParamsWithDifferentValues_whenCompared_thenAreNotEqual() {
        val params1 = MarkDeliveryFailed.Params(100L, 0)
        val params2 = MarkDeliveryFailed.Params(200L, 1)

        org.junit.Assert.assertNotEquals(params1, params2)
    }
}
