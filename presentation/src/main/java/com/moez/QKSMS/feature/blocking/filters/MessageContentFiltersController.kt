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
package org.prauga.messages.feature.blocking.filters

import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkController
import org.prauga.messages.common.util.Colors
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.injection.appComponent
import org.prauga.messages.model.MessageContentFilterData
import javax.inject.Inject

class MessageContentFiltersController :
    QkController<MessageContentFiltersView, MessageContentFiltersState,
            MessageContentFiltersPresenter>(), MessageContentFiltersView {

    @Inject
    override lateinit var presenter: MessageContentFiltersPresenter
    @Inject
    lateinit var colors: Colors

    private lateinit var add: ImageView
    private lateinit var filters: RecyclerView
    private lateinit var empty: TextView

    private val adapter = MessageContentFiltersAdapter()
    private val saveFilterSubject: Subject<MessageContentFilterData> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.message_content_filters_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.message_content_filters_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val view = containerView ?: return

        add = view.findViewById(R.id.add)
        filters = view.findViewById(R.id.filters)
        empty = view.findViewById(R.id.empty)

        add.setBackgroundTint(colors.theme().theme)
        add.setTint(colors.theme().textPrimary)
        adapter.emptyView = empty
        filters.adapter = adapter
    }

    override fun render(state: MessageContentFiltersState) {
        adapter.updateData(state.filters)
    }

    override fun removeFilter(): Observable<Long> = adapter.removeMessageContentFilter
    override fun addFilter(): Observable<*> = add.clicks()
    override fun saveFilter(): Observable<MessageContentFilterData> = saveFilterSubject

    override fun showAddDialog() {
        val layout =
            LayoutInflater.from(activity).inflate(R.layout.message_content_filters_add_dialog, null)
        val addDialog = layout.findViewById<LinearLayout>(R.id.add_dialog)
        val input = layout.findViewById<android.widget.EditText>(R.id.input)
        val caseSensitivity = layout.findViewById<PreferenceView>(R.id.caseSensitivity)
        val regexp = layout.findViewById<PreferenceView>(R.id.regexp)
        val contacts = layout.findViewById<PreferenceView>(R.id.contacts)

        (0 until addDialog.childCount)
            .map { index -> addDialog.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { Observable.merge(it) }
            .autoDispose(scope())
            .subscribe {
                it.findViewById<CompoundButton>(R.id.checkbox)?.let { checkbox ->
                    checkbox.isChecked = !checkbox.isChecked
                }
                caseSensitivity.isEnabled =
                    !(regexp.findViewById<CompoundButton>(R.id.checkbox)?.isChecked ?: false)
            }

        val dialog = AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
            .setView(layout)
            .setPositiveButton(R.string.message_content_filters_dialog_create) { _, _ ->
                var text = input.text.toString();
                if (!text.isBlank()) {
                    if (!(regexp.findViewById<CompoundButton>(R.id.checkbox)?.isChecked
                            ?: false)
                    ) text = text.trim()
                    saveFilterSubject.onNext(
                        MessageContentFilterData(
                            text,
                            (caseSensitivity.findViewById<CompoundButton>(R.id.checkbox)?.isChecked == true) &&
                                    !(regexp.findViewById<CompoundButton>(R.id.checkbox)?.isChecked
                                        ?: false),
                            regexp.findViewById<CompoundButton>(R.id.checkbox)?.isChecked == true,
                            contacts.findViewById<CompoundButton>(R.id.checkbox)?.isChecked == true
                        )
                    )
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
        dialog.show()
    }

}
