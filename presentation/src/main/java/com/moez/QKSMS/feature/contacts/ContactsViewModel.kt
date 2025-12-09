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

import android.view.inputmethod.EditorInfo
import com.uber.autodispose2.androidx.lifecycle.scope
import com.uber.autodispose2.autoDispose
import org.prauga.messages.common.base.QkViewModel
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.feature.compose.editing.ComposeItem
import org.prauga.messages.feature.compose.editing.PhoneNumberAction
import org.prauga.messages.filter.ContactFilter
import org.prauga.messages.filter.ContactGroupFilter
import org.prauga.messages.interactor.SetDefaultPhoneNumber
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.PhoneNumber
import org.prauga.messages.model.Recipient
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.PhoneNumberUtils
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.schedulers.Schedulers
import io.realm.RealmList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx3.awaitFirst
import javax.inject.Inject

class ContactsViewModel @Inject constructor(
    sharing: Boolean,
    serializedChips: HashMap<String, String?>,
    private val contactFilter: ContactFilter,
    private val contactGroupFilter: ContactGroupFilter,
    private val contactsRepo: ContactRepository,
    private val conversationRepo: ConversationRepository,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val setDefaultPhoneNumber: SetDefaultPhoneNumber
) : QkViewModel<ContactsContract, ContactsState>(ContactsState()) {

    private val contactGroups: Observable<List<ContactGroup>> by lazy { contactsRepo.getUnmanagedContactGroups() }
    private val contacts: Observable<List<Contact>> by lazy { contactsRepo.getUnmanagedContacts() }
    private val recents: Observable<List<Conversation>> by lazy {
        if (sharing) conversationRepo.getUnmanagedConversations() else Observable.just(listOf())
    }
    private val starredContacts: Observable<List<Contact>> by lazy { contactsRepo.getUnmanagedContacts(true) }

    private val selectedChips = Observable.just(serializedChips)
            .observeOn(Schedulers.io())
            .map { hashmap ->
                hashmap.map { (address, lookupKey) ->
                    Recipient(address = address, contact = lookupKey?.let(contactsRepo::getUnmanagedContact))
                }
            }

    private var shouldOpenKeyboard: Boolean = true

    override fun bindView(view: ContactsContract) {
        super.bindView(view)

        if (shouldOpenKeyboard) {
            view.openKeyboard()
            shouldOpenKeyboard = false
        }

        // Update the state's query, so we know if we should show the cancel button
        view.queryChangedIntent
                .autoDispose(view.scope())
                .subscribe { query -> newState { copy(query = query.toString()) } }

        // Clear the query
        view.queryClearedIntent
                .autoDispose(view.scope())
                .subscribe { view.clearQuery() }

        // Update the list of contact suggestions based on the query input, while also filtering out any contacts
        // that have already been selected
        Observables
                .combineLatest(
                        view.queryChangedIntent, recents, starredContacts, contactGroups, contacts, selectedChips
                ) { query, recents, starredContacts, contactGroups, contacts, selectedChips ->
                    val composeItems = mutableListOf<ComposeItem>()
                    if (query.isBlank()) {
                        composeItems += recents
                                .filter { conversation ->
                                    conversation.recipients.any { recipient ->
                                        selectedChips.none { chip ->
                                            if (recipient.contact == null) {
                                                chip.address == recipient.address
                                            } else {
                                                chip.contact?.lookupKey == recipient.contact?.lookupKey
                                            }
                                        }
                                    }
                                }
                                .map(ComposeItem::Recent)

                        composeItems += starredContacts
                                .filter { contact -> selectedChips.none { it.contact?.lookupKey == contact.lookupKey } }
                                .map(ComposeItem::Starred)

                        composeItems += contactGroups
                                .filter { group ->
                                    group.contacts.any { contact ->
                                        selectedChips.none { chip -> chip.contact?.lookupKey == contact.lookupKey }
                                    }
                                }
                                .map(ComposeItem::Group)

                        composeItems += contacts
                                .filter { contact -> selectedChips.none { it.contact?.lookupKey == contact.lookupKey } }
                                .map(ComposeItem::Person)
                    } else {
                        // If the entry is a valid destination, allow it as a recipient
                        if (phoneNumberUtils.isPossibleNumber(query.toString())) {
                            val newAddress = phoneNumberUtils.formatNumber(query)
                            val newContact = Contact(numbers = RealmList(PhoneNumber(address = newAddress)))
                            composeItems += ComposeItem.New(newContact)
                        }

                        // Strip the accents from the query. This can be an expensive operation, so
                        // cache the result instead of doing it for each contact
                        val normalizedQuery = query.removeAccents()
                        composeItems += starredContacts
                                .asSequence()
                                .filter { contact -> selectedChips.none { it.contact?.lookupKey == contact.lookupKey } }
                                .filter { contact -> contactFilter.filter(contact, normalizedQuery) }
                                .map(ComposeItem::Starred)

                        composeItems += contactGroups
                                .asSequence()
                                .filter { group ->
                                    group.contacts.any { contact ->
                                        selectedChips.none { chip -> chip.contact?.lookupKey == contact.lookupKey }
                                    }
                                }
                                .filter { group -> contactGroupFilter.filter(group, normalizedQuery) }
                                .map(ComposeItem::Group)

                        composeItems += contacts
                                .asSequence()
                                .filter { contact -> selectedChips.none { it.contact?.lookupKey == contact.lookupKey } }
                                .filter { contact -> contactFilter.filter(contact, normalizedQuery) }
                                .map(ComposeItem::Person)
                    }

                    composeItems
                }
                .subscribeOn(Schedulers.computation())
                .autoDispose(view.scope())
                .subscribe { items -> newState { copy(composeItems = items) } }

        // Listen for ComposeItems being selected, and then send them off to the number picker dialog in case
        // the user needs to select a phone number
        view.queryEditorActionIntent
                .filter { actionId -> actionId == EditorInfo.IME_ACTION_DONE }
                .withLatestFrom(state) { _, state -> state }
                .mapNotNull { state -> state.composeItems.firstOrNull() }
                .mergeWith(view.composeItemPressedIntent)
                .map { composeItem -> composeItem to false }
                .mergeWith(view.composeItemLongPressedIntent.map { composeItem -> composeItem to true })
                .observeOn(Schedulers.io())
                .map { (composeItem, force) ->
                    HashMap(composeItem.getContacts().associate { contact ->
                        if (contact.numbers.size == 1 || contact.getDefaultNumber() != null && !force) {
                            val address = contact.getDefaultNumber()?.address ?: contact.numbers[0]!!.address
                            address to contact.lookupKey
                        } else {
                            runBlocking {
                                newState { copy(selectedContact = contact) }
                                val action = view.phoneNumberActionIntent.awaitFirst()
                                newState { copy(selectedContact = null) }
                                val numberId = view.phoneNumberSelectedIntent.awaitFirst().value
                                val number = contact.numbers.find { number -> number.id == numberId }

                                if (action == PhoneNumberAction.CANCEL || number == null) {
                                    return@runBlocking null
                                }

                                if (action == PhoneNumberAction.ALWAYS) {
                                    val params = SetDefaultPhoneNumber.Params(contact.lookupKey, number.id)
                                    setDefaultPhoneNumber.execute(params)
                                }

                                number.address to contact.lookupKey
                            } ?: return@map hashMapOf<String, String?>()
                        }
                    })
                }
                .filter { result -> result.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(view.scope())
                .subscribe { result -> view.finish(result) }
    }

}
