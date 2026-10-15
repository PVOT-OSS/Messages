/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
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
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.repository.ConversationRepository

class MarkPinnedTest {

    @Mock private lateinit var conversationRepo: ConversationRepository

    @Mock private lateinit var updateBadge: UpdateBadge

    @Mock private lateinit var shortcutManager: ShortcutManager

    private lateinit var markPinned: MarkPinned

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(updateBadge.buildObservable(Unit)).thenReturn(Flowable.just(Unit))
        markPinned = MarkPinned(conversationRepo, updateBadge, shortcutManager)
    }

    @Test
    fun givenSingleThreadId_whenBuildObservable_thenMarksPinned() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(conversationRepo).markPinned(100L)
    }

    @Test
    fun givenMultipleThreadIds_whenBuildObservable_thenMarksAllPinned() {
        val threadIds = listOf(100L, 200L, 300L)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markPinned(100L, 200L, 300L)
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesShortcuts() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(shortcutManager).updateShortcuts()
    }

    @Test
    fun givenThreadIds_whenBuildObservable_thenUpdatesBadge() {
        val threadIds = listOf(100L)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(updateBadge).buildObservable(Unit)
    }

    @Test
    fun givenEmptyList_whenBuildObservable_thenStillCompletes() {
        val threadIds = emptyList<Long>()
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
    }

    @Test
    fun givenZeroThreadId_whenBuildObservable_thenMarksPinned() {
        val threadIds = listOf(0L)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markPinned(0L)
    }

    @Test
    fun givenLargeThreadId_whenBuildObservable_thenHandlesCorrectly() {
        val threadIds = listOf(Long.MAX_VALUE)
        val testSubscriber = TestSubscriber<Any>()

        markPinned.buildObservable(threadIds).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(conversationRepo).markPinned(Long.MAX_VALUE)
    }
}
