/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyList
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class CancelDelayedMessageTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    private lateinit var cancelDelayedMessage: CancelDelayedMessage

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cancelDelayedMessage = CancelDelayedMessage(conversationRepo, messageRepo)
    }

    private fun cancel() {
        val subscriber = TestSubscriber<Any>()
        cancelDelayedMessage
            .buildObservable(CancelDelayedMessage.Params(1, 9))
            .subscribe(subscriber)
        subscriber.assertComplete()
        subscriber.assertNoErrors()
    }

    @Test
    fun givenItHasNotStartedSending_whenCancelled_thenTheMessageIsDeleted() {
        `when`(messageRepo.cancelDelayedSms(1)).thenReturn(true)

        cancel()

        verify(messageRepo).deleteMessages(listOf(1L))
        verify(conversationRepo).updateConversations(9)
    }

    @Test
    fun givenItIsAlreadySending_whenCancelled_thenTheMessageIsKept() {
        `when`(messageRepo.cancelDelayedSms(1)).thenReturn(false)

        cancel()

        verify(messageRepo, never()).deleteMessages(anyList())
        verify(conversationRepo, never()).updateConversations(anyLong())
    }
}
