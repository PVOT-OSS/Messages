/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.view.View
import android.view.ViewGroup
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Message
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QkRealmAdapterSelectionTest {

    /** Items with ids 1..[size], none of them ever read */
    private class Adapter(private val size: Int, override val selectAllByRange: Boolean) :
        QkRealmAdapter<Message, QkViewHolder>() {
        var idsRead = 0

        override fun getItemCount() = size

        override fun getItemId(position: Int): Long {
            idsRead++
            return position + 1L
        }

        override fun newestId() = size.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            QkViewHolder(View(parent.context))

        override fun onBindViewHolder(holder: QkViewHolder, position: Int) = Unit

        fun tap(id: Long) = toggleSelection(id)

        fun longPressOff(id: Long) = toggleSelection(id, force = false)

        fun selected(id: Long) = isSelected(id)
    }

    private fun Adapter.observe() = TestObserver<Selection>().also { selections.subscribe(it) }

    private val TestObserver<Selection>.last
        get() = values().last()

    @Test
    fun givenABillionMessages_whenSelectingAll_thenNoIdIsListed() {
        val adapter = Adapter(1_000_000_000, selectAllByRange = true)
        val selections = adapter.observe()

        adapter.toggleSelectAll()

        assertEquals(0, adapter.idsRead)
        assertEquals(Selection(upToId = 1_000_000_000, count = 1_000_000_000), selections.last)
        assertTrue(adapter.selected(1) && adapter.selected(1_000_000_000))
        assertFalse(adapter.selected(1_000_000_001))
    }

    @Test
    fun givenEverythingSelected_whenTappingSome_thenOnlyThoseAreRemembered() {
        val adapter = Adapter(1_000_000_000, selectAllByRange = true)
        val selections = adapter.observe()
        adapter.toggleSelectAll()

        adapter.tap(7)
        adapter.tap(9)
        adapter.tap(1_000_000_005)

        assertEquals(
            Selection(1_000_000_000, setOf(7, 9), setOf(1_000_000_005), 999_999_999),
            selections.last,
        )
        assertFalse(adapter.selected(7))
        assertTrue(adapter.selected(8) && adapter.selected(1_000_000_005))

        adapter.toggleSelectAll()
        assertEquals(Selection(upToId = 1_000_000_000, count = 1_000_000_000), selections.last)

        adapter.toggleSelectAll()
        assertEquals(Selection(), selections.last)
    }

    @Test
    fun givenEverythingSelected_whenEachIsTappedOff_thenNothingIsSelected() {
        val adapter = Adapter(3, selectAllByRange = true)
        val selections = adapter.observe()
        adapter.toggleSelectAll()

        listOf(1L, 2L, 3L).forEach(adapter::tap)

        assertEquals(Selection(), selections.last)
        assertFalse(adapter.selected(1))
    }

    @Test
    fun givenAListThatListsIds_whenSelectingAll_thenEveryIdIsListedAsBefore() {
        val adapter = Adapter(5, selectAllByRange = false)
        val ids = TestObserver<List<Long>>().also { adapter.selectionChanges.subscribe(it) }

        adapter.toggleSelectAll()

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ids.values().last())
        assertTrue(adapter.selected(3))
        assertEquals(0, adapter.observe().valueCount())
    }

    @Test
    fun givenNothingSelected_whenTappingOutsideSelectionMode_thenNothingIsSelected() {
        val adapter = Adapter(5, selectAllByRange = true)

        assertFalse(adapter.longPressOff(1))
        assertFalse(adapter.selected(1))
    }
}
