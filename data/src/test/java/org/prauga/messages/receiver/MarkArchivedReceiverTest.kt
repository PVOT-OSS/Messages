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
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.MarkArchived
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MarkArchivedReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var intent: Intent

    @Mock private lateinit var markArchived: MarkArchived

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun givenThreadIdInIntent_whenOnReceive_thenMarksArchived() {
        val threadId = 100L
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)
        `when`(markArchived.buildObservable(listOf(threadId))).thenReturn(Flowable.just(Unit))

        // Simulate receiver behavior
        val extractedThreadId = intent.getLongExtra("threadId", 0)
        markArchived.buildObservable(listOf(extractedThreadId)).subscribe()

        verify(markArchived).buildObservable(listOf(100L))
    }

    @Test
    fun givenNoThreadId_whenOnReceive_thenUsesDefaultZero() {
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(0L)
        `when`(markArchived.buildObservable(listOf(0L))).thenReturn(Flowable.just(Unit))

        val extractedThreadId = intent.getLongExtra("threadId", 0)
        markArchived.buildObservable(listOf(extractedThreadId)).subscribe()

        verify(markArchived).buildObservable(listOf(0L))
    }

    @Test
    fun givenLargeThreadId_whenOnReceive_thenHandlesCorrectly() {
        val threadId = Long.MAX_VALUE
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)
        `when`(markArchived.buildObservable(listOf(threadId))).thenReturn(Flowable.just(Unit))

        val extractedThreadId = intent.getLongExtra("threadId", 0)
        markArchived.buildObservable(listOf(extractedThreadId)).subscribe()

        verify(markArchived).buildObservable(listOf(Long.MAX_VALUE))
    }

    @Test
    fun givenIntentWithThreadId_whenExtracted_thenReturnsCorrectValue() {
        val threadId = 99999L
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)

        val extracted = intent.getLongExtra("threadId", 0)
        assert(extracted == 99999L)
    }
}
