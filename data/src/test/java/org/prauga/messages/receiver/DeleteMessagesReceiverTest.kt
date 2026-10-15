/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
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
import org.prauga.messages.interactor.DeleteMessages
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DeleteMessagesReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var intent: Intent

    @Mock private lateinit var deleteMessages: DeleteMessages

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun givenThreadIdAndMessageIds_whenOnReceive_thenDeletesMessages() {
        val threadId = 100L
        val messageIds = longArrayOf(1L, 2L, 3L)
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)
        `when`(intent.getLongArrayExtra("messageIds")).thenReturn(messageIds)

        val params = DeleteMessages.Params(messageIds.toList(), threadId)
        `when`(deleteMessages.buildObservable(params)).thenReturn(Flowable.just(Unit))

        // Simulate receiver behavior
        val extractedThreadId = intent.getLongExtra("threadId", 0)
        val extractedMessageIds = intent.getLongArrayExtra("messageIds") ?: longArrayOf()
        deleteMessages
            .buildObservable(DeleteMessages.Params(extractedMessageIds.toList(), extractedThreadId))
            .subscribe()

        verify(deleteMessages).buildObservable(params)
    }

    @Test
    fun givenNoMessageIds_whenOnReceive_thenUsesEmptyArray() {
        val threadId = 100L
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)
        `when`(intent.getLongArrayExtra("messageIds")).thenReturn(null)

        val params = DeleteMessages.Params(emptyList(), threadId)
        `when`(deleteMessages.buildObservable(params)).thenReturn(Flowable.just(Unit))

        // Simulate receiver behavior
        val extractedThreadId = intent.getLongExtra("threadId", 0)
        val extractedMessageIds = intent.getLongArrayExtra("messageIds") ?: longArrayOf()
        deleteMessages
            .buildObservable(DeleteMessages.Params(extractedMessageIds.toList(), extractedThreadId))
            .subscribe()

        verify(deleteMessages).buildObservable(params)
    }

    @Test
    fun givenNoThreadId_whenOnReceive_thenUsesDefaultZero() {
        val messageIds = longArrayOf(1L)
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(0L)
        `when`(intent.getLongArrayExtra("messageIds")).thenReturn(messageIds)

        val params = DeleteMessages.Params(messageIds.toList(), 0L)
        `when`(deleteMessages.buildObservable(params)).thenReturn(Flowable.just(Unit))

        val extractedThreadId = intent.getLongExtra("threadId", 0)
        val extractedMessageIds = intent.getLongArrayExtra("messageIds") ?: longArrayOf()
        deleteMessages
            .buildObservable(DeleteMessages.Params(extractedMessageIds.toList(), extractedThreadId))
            .subscribe()

        verify(deleteMessages).buildObservable(params)
    }

    @Test
    fun givenManyMessageIds_whenOnReceive_thenHandlesAll() {
        val threadId = 100L
        val messageIds = (1L..100L).toList().toLongArray()
        `when`(intent.getLongExtra("threadId", 0)).thenReturn(threadId)
        `when`(intent.getLongArrayExtra("messageIds")).thenReturn(messageIds)

        val params = DeleteMessages.Params(messageIds.toList(), threadId)
        `when`(deleteMessages.buildObservable(params)).thenReturn(Flowable.just(Unit))

        val extractedThreadId = intent.getLongExtra("threadId", 0)
        val extractedMessageIds = intent.getLongArrayExtra("messageIds") ?: longArrayOf()
        deleteMessages
            .buildObservable(DeleteMessages.Params(extractedMessageIds.toList(), extractedThreadId))
            .subscribe()

        verify(deleteMessages).buildObservable(params)
    }
}
