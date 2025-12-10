/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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

package org.prauga.messages.feature.scheduled

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.android.AndroidInjection
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.databinding.ScheduledActivityBinding
import javax.inject.Inject

class ScheduledActivity :
    QkThemedActivity<ScheduledActivityBinding>(ScheduledActivityBinding::inflate), ScheduledView {

    @Inject
    lateinit var scheduledMessageAdapter: ScheduledMessageAdapter
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val composeIntent by lazy { binding.compose.clicks() }
    override val upgradeIntent by lazy { binding.upgrade.clicks() }
    override val messagesSelectedIntent by lazy { scheduledMessageAdapter.selectionChanges }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val deleteScheduledMessages: Subject<List<Long>> = PublishSubject.create()
    override val sendScheduledMessages: Subject<List<Long>> = PublishSubject.create()
    override val editScheduledMessage: Subject<Long> = PublishSubject.create()
    override val backPressedIntent: Subject<Unit> = PublishSubject.create()

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[ScheduledViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setTitle(R.string.scheduled_title)
        showBackButton(true)
        viewModel.bindView(this)

        scheduledMessageAdapter.emptyView = binding.empty
        binding.messages.adapter = scheduledMessageAdapter

        colors.theme().let { theme ->
            binding.sampleMessage.setBackgroundTint(theme.theme)
            binding.sampleMessage.setTextColor(theme.textPrimary)
            binding.compose.setTint(theme.textPrimary)
            binding.compose.setBackgroundTint(theme.theme)
            binding.upgrade.setBackgroundTint(theme.theme)
            binding.upgradeIcon.setTint(theme.textPrimary)
            binding.upgradeLabel.setTextColor(theme.textPrimary)
        }
    }

    override fun render(state: ScheduledState) {
        scheduledMessageAdapter.updateData(state.scheduledMessages)

        setTitle(
            when {
                (state.selectedMessages > 0) ->
                    getString(R.string.compose_title_selected, state.selectedMessages)

                else -> getString(R.string.scheduled_title)
            }
        )

        // show/hide menu items
        binding.toolbar.menu.findItem(R.id.select_all)?.isVisible =
            ((scheduledMessageAdapter.itemCount > 1) && (state.selectedMessages != 0))
        binding.toolbar.menu.findItem(R.id.delete)?.isVisible =
            ((scheduledMessageAdapter.itemCount != 0) && (state.selectedMessages != 0))
        binding.toolbar.menu.findItem(R.id.copy)?.isVisible =
            ((scheduledMessageAdapter.itemCount != 0) && (state.selectedMessages != 0))
        binding.toolbar.menu.findItem(R.id.send_now)?.isVisible =
            ((scheduledMessageAdapter.itemCount != 0) && (state.selectedMessages != 0))
        binding.toolbar.menu.findItem(R.id.edit_message)?.isVisible =
            ((scheduledMessageAdapter.itemCount != 0) && (state.selectedMessages == 1))

        // show compose button
        binding.compose.isVisible = state.upgraded && (state.conversationId == null)
        binding.upgrade.isVisible = !state.upgraded
    }

    override fun onBackPressed() = backPressedIntent.onNext(Unit)

    override fun clearSelection() = scheduledMessageAdapter.clearSelection()

    override fun toggleSelectAll() = scheduledMessageAdapter.toggleSelectAll()

    override fun showDeleteDialog(messages: List<Long>) {
        val count = messages.size
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(resources.getQuantityString(R.plurals.dialog_delete_chat, count, count))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                deleteScheduledMessages.onNext(
                    messages
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDispose(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun showSendNowDialog(messages: List<Long>) {
        val count = messages.size
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.main_menu_send_now)
            .setMessage(resources.getQuantityString(R.plurals.dialog_send_now, count, count))
            .setPositiveButton(R.string.main_menu_send_now) { _, _ ->
                sendScheduledMessages.onNext(
                    messages
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDispose(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun showEditMessageDialog(message: Long) {
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.dialog_edit_scheduled_message_title)
            .setMessage(R.string.dialog_edit_scheduled_message)
            .setPositiveButton(R.string.dialog_edit_scheduled_message_positive_button) { _, _ ->
                editScheduledMessage.onNext(message)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDispose(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scheduled_messages, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun finishActivity() {
        finish()
    }
}
