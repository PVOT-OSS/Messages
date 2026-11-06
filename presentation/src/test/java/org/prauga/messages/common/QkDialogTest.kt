/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common

import android.app.Dialog
import androidx.test.core.app.ApplicationProvider
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QkDialogTest {

    private val unhandled = mutableListOf<Throwable>()

    private class FakeDialog(private val showing: Boolean, private val dismissError: Exception?) :
        Dialog(ApplicationProvider.getApplicationContext()) {
        var dismissals = 0

        override fun isShowing() = showing

        override fun dismiss() {
            dismissals++
            dismissError?.let { throw it }
        }
    }

    @Before fun setUp() = RxJavaPlugins.setErrorHandler { unhandled += it }

    @After fun tearDown() = RxJavaPlugins.reset()

    @Test
    fun givenDialogWhoseWindowIsGone_whenMenuItemClicked_thenNoUnhandledError() {
        val clicks = PublishSubject.create<Int>()
        val dialog =
            FakeDialog(true, IllegalArgumentException("View not attached to window manager"))
        clicks.dismissOnNext(dialog)

        clicks.onNext(0)

        assertEquals(1, dialog.dismissals)
        assertTrue(unhandled.toString(), unhandled.isEmpty())
    }

    @Test
    fun givenDialogAlreadyClosed_whenMenuItemClicked_thenNotDismissedAgain() {
        val clicks = PublishSubject.create<Int>()
        val dialog = FakeDialog(false, null)
        clicks.dismissOnNext(dialog)

        clicks.onNext(0)

        assertEquals(0, dialog.dismissals)
    }
}
