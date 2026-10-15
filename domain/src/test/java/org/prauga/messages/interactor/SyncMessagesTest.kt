/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.SyncRepository

class SyncMessagesTest {

    @Mock private lateinit var syncManager: SyncRepository

    @Mock private lateinit var updateBadge: UpdateBadge

    private lateinit var syncMessages: SyncMessages

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        syncMessages = SyncMessages(syncManager, updateBadge)
    }

    @Test
    fun givenUnit_whenBuildObservable_thenSyncsMessages() {
        val testSubscriber = TestSubscriber<Any>()

        syncMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(syncManager).syncMessages()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenUpdatesBadge() {
        val testSubscriber = TestSubscriber<Any>()

        syncMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenMultipleCalls_whenBuildObservable_thenSyncsEachTime() {
        val testSubscriber1 = TestSubscriber<Any>()
        val testSubscriber2 = TestSubscriber<Any>()

        syncMessages.buildObservable(Unit).subscribe(testSubscriber1)
        syncMessages.buildObservable(Unit).subscribe(testSubscriber2)

        testSubscriber1.assertComplete()
        testSubscriber2.assertComplete()
        verify(syncManager, org.mockito.Mockito.times(2)).syncMessages()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenCompletes() {
        val testSubscriber = TestSubscriber<Any>()

        syncMessages.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
    }
}
