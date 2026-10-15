/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import android.net.Uri
import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.model.Message
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.SyncRepository

class SyncMessageTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var syncManager: SyncRepository

    @Mock private lateinit var updateBadge: UpdateBadge

    @Mock private lateinit var uri: Uri

    @Mock private lateinit var message: Message

    private lateinit var syncMessage: SyncMessage

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        syncMessage = SyncMessage(conversationRepo, syncManager, updateBadge)
    }

    @Test
    fun givenUri_whenSyncSucceeds_thenUpdatesConversation() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.threadId).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        syncMessage.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).updateConversations(threadId)
    }

    @Test
    fun givenUri_whenSyncSucceeds_thenUpdatesBadge() {
        val threadId = 100L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.threadId).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        syncMessage.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenUri_whenSyncFails_thenDoesNotUpdateConversation() {
        `when`(syncManager.syncMessage(uri)).thenReturn(null)
        val testSubscriber = TestSubscriber<Any>()

        syncMessage.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenUri_whenSyncFails_thenDoesNotUpdateBadge() {
        `when`(syncManager.syncMessage(uri)).thenReturn(null)
        val testSubscriber = TestSubscriber<Any>()

        syncMessage.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(0)
    }

    @Test
    fun givenDifferentThreadId_whenSyncSucceeds_thenUsesCorrectThreadId() {
        val threadId = 999L
        `when`(syncManager.syncMessage(uri)).thenReturn(message)
        `when`(message.threadId).thenReturn(threadId)
        val testSubscriber = TestSubscriber<Any>()

        syncMessage.buildObservable(uri).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(999L)
    }
}
