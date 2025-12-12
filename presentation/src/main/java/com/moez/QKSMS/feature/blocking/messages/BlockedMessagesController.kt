/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.prauga.messages.feature.blocking.messages

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.feature.blocking.BlockingDialog
import org.prauga.messages.injection.appComponent
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject

class BlockedMessagesController : QkController<BlockedMessagesView, BlockedMessagesState, BlockedMessagesPresenter>(),
    BlockedMessagesView {

    override val menuReadyIntent: Subject<Unit> = PublishSubject.create()
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val conversationClicks by lazy { blockedMessagesAdapter.clicks }
    override val selectionChanges by lazy { blockedMessagesAdapter.selectionChanges }
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val backClicked: Subject<Unit> = PublishSubject.create()

    @Inject lateinit var blockedMessagesAdapter: BlockedMessagesAdapter
    @Inject lateinit var blockingDialog: BlockingDialog
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context
    @Inject override lateinit var presenter: BlockedMessagesPresenter

    private lateinit var conversations: RecyclerView
    private lateinit var empty: TextView

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocked_messages_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        conversations = view.findViewById(R.id.conversations)
        empty = view.findViewById(R.id.empty)

        blockedMessagesAdapter.emptyView = empty
        conversations.adapter = blockedMessagesAdapter
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocked_messages_title)
        showBackButton(true)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.blocked_messages, menu)
        menuReadyIntent.onNext(Unit)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun handleBack(): Boolean {
        backClicked.onNext(Unit)
        return true
    }

    override fun render(state: BlockedMessagesState) {
        blockedMessagesAdapter.updateData(state.data)

        themedActivity?.toolbarView?.menu?.findItem(R.id.block)?.isVisible = state.selected > 0
        themedActivity?.toolbarView?.menu?.findItem(R.id.delete)?.isVisible = state.selected > 0

        setTitle(when (state.selected) {
            0 -> context.getString(R.string.blocked_messages_title)
            else -> context.getString(R.string.main_title_selected, state.selected)
        })
    }

    override fun clearSelection() = blockedMessagesAdapter.clearSelection()

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(activity!!, conversations, block)
    }

    override fun showDeleteDialog(conversations: List<Long>) {
        val count = conversations.size
        val dialog = AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(resources?.getQuantityString(R.plurals.dialog_delete_message, count, count))
                .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteIntent.onNext(conversations) }
                .setNegativeButton(R.string.button_cancel, null)
                .create()

        dialog.show()

        themedActivity?.theme?.take(1)
                ?.autoDisposable(scope())
                ?.subscribe { theme ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
                }
    }

    override fun goBack() {
        router.popCurrentController()
    }

}
