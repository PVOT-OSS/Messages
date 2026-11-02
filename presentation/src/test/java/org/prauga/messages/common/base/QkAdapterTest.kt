/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QkAdapterTest {

    private class TapAdapter : QkAdapter<String, QkViewHolder>() {
        val taps = mutableListOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
            val view = View(parent.context)
            return QkViewHolder(view).apply {
                view.setOnClickListener {
                    val item = getItemOrNull(adapterPosition) ?: return@setOnClickListener
                    taps += item
                }
            }
        }

        override fun onBindViewHolder(holder: QkViewHolder, position: Int) = Unit
    }

    @Test
    fun givenPositionOutsideTheList_whenGetItemOrNull_thenNull() {
        val adapter = TapAdapter().apply { data = listOf("a", "b") }

        assertNull(adapter.getItemOrNull(RecyclerView.NO_POSITION))
        assertNull(adapter.getItemOrNull(2))
        assertEquals("b", adapter.getItemOrNull(1))
    }

    @Test
    fun givenRowWithoutAdapterPosition_whenTapped_thenIgnoredInsteadOfCrash() {
        val adapter = TapAdapter().apply { data = listOf("a") }
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertEquals(RecyclerView.NO_POSITION, holder.adapterPosition)

        holder.itemView.performClick()

        assertTrue(adapter.taps.isEmpty())
    }
}
