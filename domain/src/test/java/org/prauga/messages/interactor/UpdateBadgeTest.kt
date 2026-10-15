/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.manager.WidgetManager

class UpdateBadgeTest {

    @Mock private lateinit var shortcutManager: ShortcutManager

    @Mock private lateinit var widgetManager: WidgetManager

    private lateinit var updateBadge: UpdateBadge

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        updateBadge = UpdateBadge(shortcutManager, widgetManager)
    }

    @Test
    fun givenUnit_whenBuildObservable_thenUpdatesBadge() {
        val testSubscriber = TestSubscriber<Any>()

        updateBadge.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        testSubscriber.assertNoErrors()
        verify(shortcutManager).updateBadge()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenUpdatesWidget() {
        val testSubscriber = TestSubscriber<Any>()

        updateBadge.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertComplete()
        verify(widgetManager).sendDatasetChanged()
    }

    @Test
    fun givenUnit_whenBuildObservable_thenEmitsUnit() {
        val testSubscriber = TestSubscriber<Any>()

        updateBadge.buildObservable(Unit).subscribe(testSubscriber)

        testSubscriber.assertValueCount(1)
        testSubscriber.assertValue(Unit)
    }

    @Test
    fun givenMultipleCalls_whenBuildObservable_thenUpdatesEachTime() {
        val testSubscriber1 = TestSubscriber<Any>()
        val testSubscriber2 = TestSubscriber<Any>()

        updateBadge.buildObservable(Unit).subscribe(testSubscriber1)
        updateBadge.buildObservable(Unit).subscribe(testSubscriber2)

        testSubscriber1.assertComplete()
        testSubscriber2.assertComplete()
        verify(shortcutManager, org.mockito.Mockito.times(2)).updateBadge()
        verify(widgetManager, org.mockito.Mockito.times(2)).sendDatasetChanged()
    }
}
