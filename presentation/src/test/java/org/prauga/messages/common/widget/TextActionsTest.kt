/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.widget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.text.Selection
import android.text.Spannable
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextActionsTest {

    private val noActivities =
        object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startActivity(intent: Intent) =
                throw ActivityNotFoundException("No Activity found to handle $intent")
        }

    private class GuardedTextView(context: Context) : TextView(context) {
        override fun onTextContextMenuItem(id: Int): Boolean = ignoringMissingActivity {
            super.onTextContextMenuItem(id)
        }
    }

    private fun TextView.selectAll(): TextView = apply {
        setTextIsSelectable(true)
        setText("search this", TextView.BufferType.SPANNABLE)
        Selection.setSelection(text as Spannable, 0, text.length)
    }

    @Test
    fun givenNoAppForTheAction_whenPlainTextViewRunsIt_thenItCrashes() {
        val view = TextView(noActivities).selectAll()

        assertThrows(ActivityNotFoundException::class.java) {
            view.onTextContextMenuItem(android.R.id.shareText)
        }
    }

    @Test
    fun givenNoAppForTheAction_whenGuardedTextViewRunsIt_thenIgnored() {
        val view = GuardedTextView(noActivities).selectAll()

        assertFalse(view.onTextContextMenuItem(android.R.id.shareText))
    }
}
