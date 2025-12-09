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

package org.prauga.messages.feature.contacts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.editorActions
import com.jakewharton.rxbinding4.widget.textChanges
import dagger.android.AndroidInjection
import org.prauga.messages.R
import org.prauga.messages.common.ViewModelFactory
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.common.util.extensions.hideKeyboard
import org.prauga.messages.common.util.extensions.showKeyboard
import org.prauga.messages.common.widget.QkDialog
import org.prauga.messages.databinding.ContactsActivityBinding
import org.prauga.messages.extensions.Optional
import org.prauga.messages.feature.compose.editing.ComposeItem
import org.prauga.messages.feature.compose.editing.ComposeItemAdapter
import org.prauga.messages.feature.compose.editing.PhoneNumberAction
import org.prauga.messages.feature.compose.editing.PhoneNumberPickerAdapter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import javax.inject.Inject

class ContactsActivity :
    QkThemedActivity<ContactsActivityBinding>(ContactsActivityBinding::inflate), ContactsContract {

    companion object {
        const val SharingKey = "sharing"
        const val ChipsKey = "chips"
    }

    @Inject
    lateinit var contactsAdapter: ComposeItemAdapter
    @Inject
    lateinit var phoneNumberAdapter: PhoneNumberPickerAdapter
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    override val queryChangedIntent: Observable<CharSequence> by lazy { binding.search.textChanges() }
    override val queryClearedIntent: Observable<*> by lazy { binding.cancel.clicks() }
    override val queryEditorActionIntent: Observable<Int> by lazy { binding.search.editorActions() }
    override val composeItemPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.clicks }
    override val composeItemLongPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.longClicks }
    override val phoneNumberSelectedIntent: Subject<Optional<Long>> by lazy { phoneNumberAdapter.selectedItemChanges }
    override val phoneNumberActionIntent: Subject<PhoneNumberAction> = PublishSubject.create()

    private val viewModel by lazy {
        ViewModelProviders.of(
            this,
            viewModelFactory
        )[ContactsViewModel::class.java]
    }

    private val phoneNumberDialog by lazy {
        QkDialog(this).apply {
            titleRes = R.string.compose_number_picker_title
            adapter = phoneNumberAdapter
            positiveButton = R.string.compose_number_picker_always
            positiveButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.ALWAYS) }
            negativeButton = R.string.compose_number_picker_once
            negativeButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.JUST_ONCE) }
            cancelListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.CANCEL) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        showBackButton(true)
        viewModel.bindView(this)

        binding.contacts.adapter = contactsAdapter
    }

    override fun render(state: ContactsState) {
        binding.cancel.isVisible = state.query.length > 1

        contactsAdapter.data = state.composeItems

        if (state.selectedContact != null && !phoneNumberDialog.isShowing) {
            phoneNumberAdapter.data = state.selectedContact.numbers
            phoneNumberDialog.subtitle = state.selectedContact.name
            phoneNumberDialog.show()
        } else if (state.selectedContact == null && phoneNumberDialog.isShowing) {
            phoneNumberDialog.dismiss()
        }
    }

    override fun clearQuery() {
        binding.search.text = null
    }

    override fun openKeyboard() {
        binding.search.postDelayed({
            binding.search.showKeyboard()
        }, 200)
    }

    override fun finish(result: HashMap<String, String?>) {
        binding.search.hideKeyboard()
        val intent = Intent().putExtra(ChipsKey, result)
        setResult(RESULT_OK, intent)
        super.finish()
    }

    override fun finish() {
        // Set empty result if no result was already set
        val intent = Intent().putExtra(ChipsKey, hashMapOf<String, String?>())
        setResult(RESULT_OK, intent)
        super.finish()
    }
}
