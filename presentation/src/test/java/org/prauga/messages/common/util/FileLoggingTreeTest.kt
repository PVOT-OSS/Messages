/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.util.Preferences
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class FileLoggingTreeTest {

    private lateinit var prefs: Preferences
    private lateinit var tree: FileLoggingTree

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sharedPrefs = context.getSharedPreferences("file-logging-test", 0)
        prefs = Preferences(context, RxSharedPreferences.create(sharedPrefs), sharedPrefs)
        tree = FileLoggingTree(prefs, context)
    }

    @After
    fun tearDown() = Timber.uprootAll()

    @Test
    fun givenFileLoggingOn_whenLoggingABurst_thenThreadsAndQueueStayBounded() {
        prefs.logging.set(true)
        val threadsBefore = Thread.activeCount()

        repeat(20_000) {
            tree.log(android.util.Log.DEBUG, "Test", "line $it", null)
            assertTrue(tree.pendingWrites <= FileLoggingTree.MAX_PENDING_WRITES)
        }

        assertTrue(Thread.activeCount() - threadsBefore <= 1)
    }

    @Test
    fun givenLoggingSetting_whenItChanges_thenTreeIsPlantedOnlyWhileOn() {
        val enabled = PublishSubject.create<Boolean>()
        tree.plantWhile(enabled)

        enabled.onNext(false)
        assertFalse(tree in Timber.forest())

        enabled.onNext(true)
        enabled.onNext(true)
        assertEquals(1, Timber.forest().count { it === tree })

        enabled.onNext(false)
        assertFalse(tree in Timber.forest())
    }
}
