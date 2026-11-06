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

    /** A list of any size that remembers how many of its items were read */
    private class CountingList(override val size: Int) : AbstractList<String>() {
        var reads = 0

        override fun get(index: Int): String {
            reads++
            return "item $index"
        }
    }

    private class Changes : RecyclerView.AdapterDataObserver() {
        var full = 0
        var inserted = 0

        override fun onChanged() {
            full++
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            inserted += itemCount
        }
    }

    @Test
    fun givenAHugeList_whenSet_thenItIsNotDiffedItemByItem() {
        val adapter = TapAdapter().apply { data = listOf("a") }
        val changes = Changes().also(adapter::registerAdapterDataObserver)
        val huge = CountingList(1_000_000_000)

        adapter.data = huge

        assertEquals(0, huge.reads)
        assertEquals(1, changes.full)
        assertEquals(1_000_000_000, adapter.itemCount)
    }

    @Test
    fun givenASmallList_whenSet_thenOnlyTheDifferenceIsDispatched() {
        val adapter = TapAdapter().apply { data = listOf("a") }
        val changes = Changes().also(adapter::registerAdapterDataObserver)

        adapter.data = listOf("a", "b", "c")

        assertEquals(0, changes.full)
        assertEquals(2, changes.inserted)
    }
}
