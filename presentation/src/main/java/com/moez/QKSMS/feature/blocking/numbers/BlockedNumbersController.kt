/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.injection.appComponent
import org.prauga.messages.util.PhoneNumberUtils

class BlockedNumbersController :
    QkController<BlockedNumbersView, BlockedNumbersState, BlockedNumbersPresenter>(),
    BlockedNumbersView {

    @Inject override lateinit var presenter: BlockedNumbersPresenter
    @Inject lateinit var colors: Colors
    @Inject lateinit var phoneNumberUtils: PhoneNumberUtils

    private lateinit var add: ImageView
    private lateinit var numbers: RecyclerView
    private lateinit var empty: TextView

    private val adapter = BlockedNumbersAdapter()
    private val saveAddressSubject: Subject<String> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocked_numbers_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocked_numbers_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        add = view.findViewById(R.id.add)
        numbers = view.findViewById(R.id.numbers)
        empty = view.findViewById(R.id.empty)

        add.setBackgroundTint(colors.theme().theme)
        add.setTint(colors.theme().textPrimary)
        adapter.emptyView = empty
        numbers.adapter = adapter
    }

    override fun render(state: BlockedNumbersState) {
        adapter.updateData(state.numbers)
    }

    override fun unblockAddress(): Observable<Long> = adapter.unblockAddress

    override fun addAddress(): Observable<*> = add.clicks()

    override fun saveAddress(): Observable<String> = saveAddressSubject

    override fun showAddDialog() {
        val layout =
            LayoutInflater.from(activity).inflate(R.layout.blocked_numbers_add_dialog, null)
        val input = layout.findViewById<android.widget.EditText>(R.id.input)
        val textWatcher = BlockedNumberTextWatcher(input, phoneNumberUtils)
        val dialog =
            AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setView(layout)
                .setPositiveButton(R.string.blocked_numbers_dialog_block) { _, _ ->
                    saveAddressSubject.onNext(input.text.toString())
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> }
                .setOnDismissListener { textWatcher.dispose() }
                .create()

        dialog.show()

        themedActivity?.theme?.take(1)?.autoDispose(scope())?.subscribe { theme ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
        }
    }
}
