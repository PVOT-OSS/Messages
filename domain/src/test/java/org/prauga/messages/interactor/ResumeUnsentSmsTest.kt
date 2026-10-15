/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import android.provider.Telephony
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository

class ResumeUnsentSmsTest {

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var notificationManager: NotificationManager

    private lateinit var resumeUnsentSms: ResumeUnsentSms

    private val now = 1_000_000L

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        resumeUnsentSms = ResumeUnsentSms(messageRepo, notificationManager)
    }

    private fun sms(id: Long, date: Long): Message =
        mock(Message::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.date).thenReturn(date)
        }

    private fun resume(retrying: Set<Long> = emptySet()) {
        val subscriber = TestSubscriber<Unit>()
        resumeUnsentSms.buildObservable(ResumeUnsentSms.Params(now, retrying)).subscribe(subscriber)
        subscriber.assertComplete()
        subscriber.assertNoErrors()
    }

    @Test
    fun givenDelayedSmsStillToSend_whenResumed_thenItsSendIsScheduledAgain() {
        val unsent = listOf(sms(1, now + 5_000))
        `when`(messageRepo.getUnsentSms()).thenReturn(unsent)

        resume()

        verify(messageRepo).scheduleDelayedSms(1, now + 5_000)
        verify(messageRepo, never()).markFailed(anyLong(), anyInt())
    }

    @Test
    fun givenSmsInterruptedWhileSending_whenResumed_thenItFailsWithoutBeingResent() {
        val unsent = listOf(sms(2, now - 5_000))
        `when`(messageRepo.getUnsentSms()).thenReturn(unsent)

        resume()

        verify(messageRepo).markFailed(2, Telephony.MmsSms.ERR_TYPE_GENERIC)
        verify(notificationManager).notifyFailed(2)
        verify(messageRepo, never()).scheduleDelayedSms(anyLong(), anyLong())
        verify(messageRepo, never()).markSending(anyLong())
    }

    @Test
    fun givenBoth_whenResumed_thenEachIsHandled() {
        val unsent = listOf(sms(1, now + 1), sms(2, now), sms(3, now - 1))
        `when`(messageRepo.getUnsentSms()).thenReturn(unsent)

        resume()

        verify(messageRepo).scheduleDelayedSms(1, now + 1)
        verify(messageRepo).markFailed(2, Telephony.MmsSms.ERR_TYPE_GENERIC)
        verify(messageRepo).markFailed(3, Telephony.MmsSms.ERR_TYPE_GENERIC)
    }

    @Test
    fun givenNothingUnsent_whenResumed_thenNothingChanges() {
        `when`(messageRepo.getUnsentSms()).thenReturn(emptyList())

        resume()

        verify(messageRepo, never()).scheduleDelayedSms(anyLong(), anyLong())
        verify(messageRepo, never()).markFailed(anyLong(), anyInt())
    }

    @Test
    fun givenTheLookupFails_whenResumed_thenItStillCompletes() {
        `when`(messageRepo.getUnsentSms()).thenThrow(IllegalStateException("Realm"))

        resume()
    }

    @Test
    fun givenSmsWaitingForAnAutomaticRetry_whenResumed_thenItIsLeftToTheRetry() {
        val unsent = listOf(sms(4, now - 5_000))
        `when`(messageRepo.getUnsentSms()).thenReturn(unsent)

        resume(retrying = setOf(4))

        verify(messageRepo, never()).scheduleDelayedSms(anyLong(), anyLong())
        verify(messageRepo, never()).markFailed(anyLong(), anyInt())
    }

    @Test
    fun givenADelayedSmsWhoseTimePassedWhileOff_whenResumed_thenItGoesNow() {
        val unsent = listOf(sms(5, now - 5_000))
        `when`(messageRepo.getUnsentSms()).thenReturn(unsent)
        `when`(messageRepo.isDelayedSmsWaiting(5)).thenReturn(true)

        resume()

        verify(messageRepo).scheduleDelayedSms(5, now)
        verify(messageRepo, never()).markFailed(anyLong(), anyInt())
    }
}
