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
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.MessageRepository

class MarkAllSeenTest {

    @Mock private lateinit var messageRepo: MessageRepository

    private lateinit var markAllSeen: MarkAllSeen

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        markAllSeen = MarkAllSeen(messageRepo)
    }

    @Test
    fun givenUnit_whenBuildObservable_thenMarksAllSeen() {
        val testSubscriber = TestSubscriber<Unit>()

        markAllSeen.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(messageRepo).markAllSeen()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenEmitsUnit() {
        val testSubscriber = TestSubscriber<Unit>()

        markAllSeen.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertValueCount(1)
        testSubscriber.assertValue(Unit)
    }

    @Test
    fun givenMultipleCalls_whenBuildObservable_thenCallsRepositoryEachTime() {
        val testSubscriber1 = TestSubscriber<Unit>()
        val testSubscriber2 = TestSubscriber<Unit>()

        markAllSeen.buildObservable(Unit).subscribe(testSubscriber1)
        markAllSeen.buildObservable(Unit).subscribe(testSubscriber2)

        testSubscriber1.assertComplete()
        testSubscriber2.assertComplete()
        verify(messageRepo, org.mockito.Mockito.times(2)).markAllSeen()
    }
}
