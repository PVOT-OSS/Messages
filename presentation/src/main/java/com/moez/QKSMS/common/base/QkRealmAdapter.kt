/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.OrderedRealmCollection
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import org.prauga.messages.common.util.extensions.setVisible
import timber.log.Timber

abstract class QkRealmAdapter<T : RealmModel, VH : QkViewHolder> :
    RealmRecyclerViewAdapter<T, VH>(null, true) {

    /**
     * This view can be set, and the adapter will automatically control the visibility of this view
     * based on the data
     */
    var emptyView: View? = null
        set(value) {
            if (field === value) return

            field = value
            value?.setVisible(data?.isLoaded == true && data?.isEmpty() == true)
        }

    private val emptyListener: (OrderedRealmCollection<T>) -> Unit = { data ->
        emptyView?.setVisible(data.isLoaded && data.isEmpty())
    }

    val selectionChanges: Subject<List<Long>> = BehaviorSubject.create()

    /** Emitted on every change by adapters that [selectAllByRange] */
    val selections: Subject<Selection> = BehaviorSubject.create()

    /** Select all by remembering the newest id, instead of listing every id */
    protected open val selectAllByRange = false

    private var selection = mutableListOf<Long>()
    private val selected = hashSetOf<Long>()

    /** While everything is selected, the newest id and how many items there were */
    private var upToId = 0L
    private var rangeCount = 0

    /** Mark this message as highlighted */
    var highlight: Long = -1L
        set(value) {
            if (field == value) return

            field = value
            notifyDataSetChanged()
        }

    private fun selectedCount(): Int =
        when (upToId) {
            0L -> selection.size
            else ->
                rangeCount - selection.count { id -> id <= upToId } +
                    selection.count { id -> id > upToId }
        }

    /**
     * Toggles the selected state for a particular view
     *
     * If we are currently in selection mode (we have an active selection), then the state will
     * toggle. If we are not in selection mode, then we will only toggle if [force]
     */
    protected fun toggleSelection(id: Long, force: Boolean = true): Boolean {
        if (!force && selectedCount() == 0) return false

        when (selected.remove(id)) {
            true -> selection -= id
            false -> {
                selected += id
                selection += id
            }
        }
        if (upToId != 0L && selectedCount() == 0) {
            upToId = 0
            selection.clear()
            selected.clear()
        }

        emitSelection()

        return true
    }

    protected fun isSelected(id: Long): Boolean {
        return if (id <= upToId) id !in selected else id in selected
    }

    fun clearSelection() {
        upToId = 0
        selection.clear()
        selected.clear()
        emitSelection()
        notifyDataSetChanged()
    }

    fun toggleSelectAll() {
        val needToSelectAll = (selectedCount() != itemCount)

        upToId = 0
        selection.clear()
        selected.clear()

        if (needToSelectAll) {
            val newest = if (selectAllByRange) newestId() else null
            if (newest != null && newest > 0) {
                upToId = newest
                rangeCount = itemCount
            } else {
                for (position in 0 until itemCount) {
                    val id = getItemId(position)
                    if (selected.add(id)) selection += id
                }
            }
        }

        // fire a single change event now
        emitSelection()

        notifyDataSetChanged()
    }

    protected open fun newestId(): Long? = (data as? RealmResults<T>)?.max("id")?.toLong()

    private fun emitSelection() {
        selectionChanges.onNext(selection)
        if (!selectAllByRange) return

        val (except, picked) = selection.partition { id -> id <= upToId }
        selections.onNext(Selection(upToId, except.toSet(), picked.toSet(), selectedCount()))
    }

    override fun getItem(index: Int): T? {
        if (index < 0) {
            Timber.w("Only indexes >= 0 are allowed. Input was: $index")
            return null
        }

        return super.getItem(index)
    }

    override fun updateData(data: OrderedRealmCollection<T>?) {
        if (getData() === data) return

        // A range only describes the items it was made from
        if (upToId != 0L) clearSelection()

        removeListener(getData())
        addListener(data)

        data?.run(emptyListener)

        super.updateData(data)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        addListener(data)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        removeListener(data)
    }

    private fun addListener(data: OrderedRealmCollection<T>?) {
        when (data) {
            is RealmResults<T> -> data.addChangeListener(emptyListener)
            is RealmList<T> -> data.addChangeListener(emptyListener)
        }
    }

    private fun removeListener(data: OrderedRealmCollection<T>?) {
        when (data) {
            is RealmResults<T> -> data.removeChangeListener(emptyListener)
            is RealmList<T> -> data.removeChangeListener(emptyListener)
        }
    }
}
