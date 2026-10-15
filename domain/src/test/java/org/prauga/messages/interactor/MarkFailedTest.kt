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
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.repository.MessageRepository

class MarkFailedTest {

    @Mock private lateinit var messageRepository: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    private lateinit var markFailed: MarkFailed

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markFailed = MarkFailed(messageRepository, notificationManager)
    }

    @Test
    fun givenParams_whenBuildObservable_thenCallsMarkFailedAndNotifies() {
        val messageId = 123L
        val resultCode = -1
        val params = MarkFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markFailed(messageId, resultCode)
        verify(notificationManager).notifyFailed(messageId)
    }

    @Test
    fun givenZeroResultCode_whenBuildObservable_thenCallsWithCorrectCode() {
        val messageId = 456L
        val resultCode = 0
        val params = MarkFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markFailed(messageId, resultCode)
        verify(notificationManager).notifyFailed(messageId)
    }

    @Test
    fun givenGenericFailureResultCode_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = 789L
        val resultCode = 1 // RESULT_ERROR_GENERIC_FAILURE
        val params = MarkFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markFailed(messageId, resultCode)
        verify(notificationManager).notifyFailed(messageId)
    }

    @Test
    fun givenNoServiceResultCode_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = 100L
        val resultCode = 4 // RESULT_ERROR_NO_SERVICE
        val params = MarkFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markFailed(messageId, resultCode)
        verify(notificationManager).notifyFailed(messageId)
    }

    @Test
    fun givenRadioOffResultCode_whenBuildObservable_thenHandlesCorrectly() {
        val messageId = 200L
        val resultCode = 2 // RESULT_ERROR_RADIO_OFF
        val params = MarkFailed.Params(messageId, resultCode)
        val testSubscriber = TestSubscriber<Unit>()

        markFailed.buildObservable(params).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markFailed(messageId, resultCode)
        verify(notificationManager).notifyFailed(messageId)
    }

    @Test
    fun givenParamsDataClass_whenCreated_thenContainsCorrectValues() {
        val id = 999L
        val resultCode = -5
        val params = MarkFailed.Params(id, resultCode)

        org.junit.Assert.assertEquals(id, params.id)
        org.junit.Assert.assertEquals(resultCode, params.resultCode)
    }

    @Test
    fun givenTwoParamsWithSameValues_whenCompared_thenAreEqual() {
        val params1 = MarkFailed.Params(100L, 1)
        val params2 = MarkFailed.Params(100L, 1)

        org.junit.Assert.assertEquals(params1, params2)
        org.junit.Assert.assertEquals(params1.hashCode(), params2.hashCode())
    }

    @Test
    fun givenTwoParamsWithDifferentValues_whenCompared_thenAreNotEqual() {
        val params1 = MarkFailed.Params(100L, 1)
        val params2 = MarkFailed.Params(200L, 2)

        org.junit.Assert.assertNotEquals(params1, params2)
    }
}
