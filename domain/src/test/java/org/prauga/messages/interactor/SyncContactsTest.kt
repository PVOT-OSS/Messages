/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.repository.SyncRepository

class SyncContactsTest {

    @Mock private lateinit var syncManager: SyncRepository

    private lateinit var syncContacts: SyncContacts

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        syncContacts = SyncContacts(syncManager)
    }

    @Test
    fun givenUnit_whenBuildObservable_thenSyncsContacts() {
        val testSubscriber = TestSubscriber<Long>()

        syncContacts.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(syncManager).syncContacts()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenEmitsDuration() {
        val testSubscriber = TestSubscriber<Long>()

        syncContacts.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertValueCount(1)
    }

    @Test
    fun givenMultipleCalls_whenBuildObservable_thenSyncsEachTime() {
        val testSubscriber1 = TestSubscriber<Long>()
        val testSubscriber2 = TestSubscriber<Long>()

        syncContacts.buildObservable(Unit).subscribe(testSubscriber1)
        syncContacts.buildObservable(Unit).subscribe(testSubscriber2)

        testSubscriber1.assertComplete()
        testSubscriber2.assertComplete()
        verify(syncManager, org.mockito.Mockito.times(2)).syncContacts()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenDurationIsNonNegative() {
        val testSubscriber = TestSubscriber<Long>()

        syncContacts.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        val duration = testSubscriber.values().first()
        assert(duration >= 0) { "Duration should be non-negative" }
    }
}
