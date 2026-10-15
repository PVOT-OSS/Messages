/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.net.Uri
import io.reactivex.Observable
import io.reactivex.Single
import io.realm.RealmResults
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup

interface ContactRepository {

    fun findContactUri(address: String): Single<Uri>

    fun getContacts(): RealmResults<Contact>

    fun getUnmanagedContact(lookupKey: String): Contact?

    fun getUnmanagedAllContacts(): List<Contact>

    fun getUnmanagedContacts(starred: Boolean = false): Observable<List<Contact>>

    fun getUnmanagedContactGroups(): Observable<List<ContactGroup>>

    fun setDefaultPhoneNumber(lookupKey: String, phoneNumberId: Long)

    fun isContact(address: String): Boolean
}
