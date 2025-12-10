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
package org.prauga.messages.feature.compose.editing

import io.realm.RealmList
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.PhoneNumber

sealed class ComposeItem {

    abstract fun getContacts(): List<Contact>

    data class New(val value: Contact) : ComposeItem() {
        override fun getContacts(): List<Contact> = listOf(value)
    }

    data class Recent(val value: Conversation) : ComposeItem() {
        override fun getContacts(): List<Contact> = value.recipients.map { recipient ->
            recipient.contact
                ?: Contact(numbers = RealmList(PhoneNumber(address = recipient.address)))
        }
    }

    data class Starred(val value: Contact) : ComposeItem() {
        override fun getContacts(): List<Contact> = listOf(value)
    }

    data class Group(val value: ContactGroup) : ComposeItem() {
        override fun getContacts(): List<Contact> = value.contacts
    }

    data class Person(val value: Contact) : ComposeItem() {
        override fun getContacts(): List<Contact> = listOf(value)
    }
}
