/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.Preferences

class DeleteOldMessagesTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var messageRepo: MessageRepository

    @Mock private lateinit var prefs: Preferences

    @Mock private lateinit var autoDeletePref: Preference<Int>

    private lateinit var deleteOldMessages: DeleteOldMessages

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(prefs.autoDelete).thenReturn(autoDeletePref)
        deleteOldMessages = DeleteOldMessages(conversationRepo, messageRepo, prefs)
    }

    @Test
    fun givenAutoDeleteDisabled_whenBuildObservable_thenDoesNotDelete() {
        `when`(autoDeletePref.get()).thenReturn(0)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
    }

    @Test
    fun givenAutoDeleteNegative_whenBuildObservable_thenDoesNotDelete() {
        `when`(autoDeletePref.get()).thenReturn(-1)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenAutoDeleteEnabled_whenBuildObservable_thenDeletesOldMessages() {
        val maxAge = 30
        val counts = mapOf(100L to 5, 200L to 3)
        `when`(autoDeletePref.get()).thenReturn(maxAge)
        `when`(messageRepo.getOldMessageCounts(maxAge)).thenReturn(counts)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteOldMessages(maxAge)
    }

    @Test
    fun givenAutoDeleteEnabled_whenBuildObservable_thenUpdatesConversations() {
        val maxAge = 30
        val counts = mapOf(100L to 5, 200L to 3)
        `when`(autoDeletePref.get()).thenReturn(maxAge)
        `when`(messageRepo.getOldMessageCounts(maxAge)).thenReturn(counts)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).updateConversations(100L, 200L)
    }

    @Test
    fun givenNoOldMessages_whenBuildObservable_thenStillCompletes() {
        val maxAge = 30
        val counts = emptyMap<Long, Int>()
        `when`(autoDeletePref.get()).thenReturn(maxAge)
        `when`(messageRepo.getOldMessageCounts(maxAge)).thenReturn(counts)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteOldMessages(maxAge)
    }

    @Test
    fun givenLargeMaxAge_whenBuildObservable_thenHandlesCorrectly() {
        val maxAge = 365
        val counts = mapOf(100L to 1000)
        `when`(autoDeletePref.get()).thenReturn(maxAge)
        `when`(messageRepo.getOldMessageCounts(maxAge)).thenReturn(counts)
        val testSubscriber = TestSubscriber<Any>()

        deleteOldMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(messageRepo).deleteOldMessages(365)
    }
}
