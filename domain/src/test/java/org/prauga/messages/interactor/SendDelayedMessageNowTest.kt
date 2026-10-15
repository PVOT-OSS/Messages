/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyList
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class SendDelayedMessageNowTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    private lateinit var sendDelayedMessageNow: SendDelayedMessageNow

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sendDelayedMessageNow = SendDelayedMessageNow(conversationRepo, messageRepo)
    }

    @Test
    fun givenADelayedMessage_whenSentNow_thenTheSameMessageIsSent() {
        val subscriber = TestSubscriber<Any>()

        sendDelayedMessageNow
            .buildObservable(SendDelayedMessageNow.Params(1, 9))
            .subscribe(subscriber)

        subscriber.assertComplete()
        subscriber.assertNoErrors()
        verify(messageRepo).sendDelayedSmsNow(1)
        verify(conversationRepo).updateConversations(9)
    }

    @Test
    fun givenADelayedMessage_whenSentNow_thenItIsNotReplacedByACopy() {
        sendDelayedMessageNow.buildObservable(SendDelayedMessageNow.Params(1, 9)).subscribe()

        verify(messageRepo, never()).deleteMessages(anyList())
    }
}
