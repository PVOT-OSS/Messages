/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.filter

import javax.inject.Inject
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.ContactGroup

class ContactGroupFilter @Inject constructor(private val contactFilter: ContactFilter) :
    Filter<ContactGroup>() {

    override fun filter(item: ContactGroup, query: CharSequence): Boolean {
        return item.title.removeAccents().contains(query, true) || // Name
            item.contacts.any { contact -> contactFilter.filter(contact, query) } // Contacts
    }
}
