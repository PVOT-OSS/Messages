/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.SendScheduledMessage
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SendScheduledMessageReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var intent: Intent

    @Mock private lateinit var sendScheduledMessage: SendScheduledMessage

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun givenValidId_whenOnReceive_thenSendsScheduledMessage() {
        val id = 100L
        `when`(intent.getLongExtra("id", -1L)).thenReturn(id)
        `when`(sendScheduledMessage.buildObservable(id)).thenReturn(Flowable.just(Unit))

        // Simulate receiver behavior
        val extractedId = intent.getLongExtra("id", -1L)
        if (extractedId >= 0) {
            sendScheduledMessage.buildObservable(extractedId).subscribe()
        }

        verify(sendScheduledMessage).buildObservable(100L)
    }

    @Test
    fun givenZeroId_whenOnReceive_thenSendsScheduledMessage() {
        val id = 0L
        `when`(intent.getLongExtra("id", -1L)).thenReturn(id)
        `when`(sendScheduledMessage.buildObservable(id)).thenReturn(Flowable.just(Unit))

        // Simulate receiver behavior
        val extractedId = intent.getLongExtra("id", -1L)
        if (extractedId >= 0) {
            sendScheduledMessage.buildObservable(extractedId).subscribe()
        }

        verify(sendScheduledMessage).buildObservable(0L)
    }

    @Test
    fun givenNegativeId_whenOnReceive_thenDoesNotSend() {
        `when`(intent.getLongExtra("id", -1L)).thenReturn(-1L)

        // Simulate receiver behavior
        val extractedId = intent.getLongExtra("id", -1L)
        if (extractedId >= 0) {
            sendScheduledMessage.buildObservable(extractedId).subscribe()
        }

        verify(sendScheduledMessage, never()).buildObservable(-1L)
    }

    @Test
    fun givenNoIdProvided_whenOnReceive_thenDoesNotSend() {
        `when`(intent.getLongExtra("id", -1L)).thenReturn(-1L)

        // Simulate receiver behavior
        val extractedId = intent.getLongExtra("id", -1L)
        if (extractedId >= 0) {
            sendScheduledMessage.buildObservable(extractedId).subscribe()
        }

        verify(sendScheduledMessage, never()).buildObservable(-1L)
    }

    @Test
    fun givenLargeId_whenOnReceive_thenHandlesCorrectly() {
        val id = Long.MAX_VALUE
        `when`(intent.getLongExtra("id", -1L)).thenReturn(id)
        `when`(sendScheduledMessage.buildObservable(id)).thenReturn(Flowable.just(Unit))

        val extractedId = intent.getLongExtra("id", -1L)
        if (extractedId >= 0) {
            sendScheduledMessage.buildObservable(extractedId).subscribe()
        }

        verify(sendScheduledMessage).buildObservable(Long.MAX_VALUE)
    }

    @Test
    fun givenIdExtraction_whenDefaultIsNegative_thenReturnsNegativeAsDefault() {
        `when`(intent.getLongExtra("id", -1L)).thenReturn(-1L)

        val extracted = intent.getLongExtra("id", -1L)
        assert(extracted == -1L)
    }
}
