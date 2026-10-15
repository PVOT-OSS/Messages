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
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class RetrySendingTest {

    @Mock private lateinit var messageRepository: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var smsMessage: Message

    @Mock private lateinit var mmsMessage: Message

    private lateinit var retrySending: RetrySending

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        retrySending = RetrySending(messageRepository, notificationManager)
    }

    @Test
    fun givenSmsMessageId_whenBuildObservable_thenMarksSendingAndSendsSms() {
        val messageId = 123L
        val threadId = 456L
        `when`(messageRepository.getMessage(messageId)).thenReturn(smsMessage)
        `when`(smsMessage.isSms()).thenReturn(true)
        `when`(smsMessage.threadId).thenReturn(threadId)

        val testSubscriber = TestSubscriber<Message>()
        retrySending.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markSending(messageId)
        verify(messageRepository).getMessage(messageId)
        verify(messageRepository).sendSms(smsMessage)
        verify(notificationManager).cancel(threadId.toInt() + 100000)
    }

    @Test
    fun givenMmsMessageId_whenBuildObservable_thenMarksSendingAndResendsMms() {
        val messageId = 789L
        val threadId = 101L
        `when`(messageRepository.getMessage(messageId)).thenReturn(mmsMessage)
        `when`(mmsMessage.isSms()).thenReturn(false)
        `when`(mmsMessage.threadId).thenReturn(threadId)

        val testSubscriber = TestSubscriber<Message>()
        retrySending.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepository).markSending(messageId)
        verify(messageRepository).getMessage(messageId)
        verify(messageRepository).resendMms(mmsMessage)
        verify(notificationManager).cancel(threadId.toInt() + 100000)
    }

    @Test
    fun givenNonExistentMessageId_whenBuildObservable_thenCompletesWithoutSending() {
        val messageId = 999L
        `when`(messageRepository.getMessage(messageId)).thenReturn(null)

        val testSubscriber = TestSubscriber<Message>()
        retrySending.buildObservable(messageId).subscribe(testSubscriber)

        // When message doesn't exist, the observable should complete without emitting
        // The mapNotNull filters out the null result, so no downstream actions occur
        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0) // No values emitted since message was null
        verify(messageRepository).markSending(messageId)
        verify(messageRepository).getMessage(messageId)
    }

    @Test
    fun givenZeroMessageId_whenBuildObservable_thenStillProcesses() {
        val messageId = 0L
        `when`(messageRepository.getMessage(messageId)).thenReturn(null)

        val testSubscriber = TestSubscriber<Message>()
        retrySending.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepository).markSending(messageId)
    }

    @Test
    fun givenLargeThreadId_whenSendingSms_thenCancelsCorrectNotification() {
        val messageId = 100L
        val threadId = 50000L
        `when`(messageRepository.getMessage(messageId)).thenReturn(smsMessage)
        `when`(smsMessage.isSms()).thenReturn(true)
        `when`(smsMessage.threadId).thenReturn(threadId)

        val testSubscriber = TestSubscriber<Message>()
        retrySending.buildObservable(messageId).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        // Notification ID should be threadId + 100000
        verify(notificationManager).cancel(threadId.toInt() + 100000)
    }
}
