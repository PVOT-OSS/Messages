/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject
import javax.inject.Singleton
import org.prauga.messages.extensions.asFlowable
import org.prauga.messages.extensions.asObservable
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.model.Contact
import org.prauga.messages.model.ContactGroup
import org.prauga.messages.util.Preferences

@Singleton
class ContactRepositoryImpl
@Inject
constructor(private val context: Context, private val prefs: Preferences) : ContactRepository {

    override fun findContactUri(address: String): Single<Uri> {
        return Flowable.just(address)
            .map {
                when {
                    address.contains('@') -> {
                        Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(address))
                    }

                    else -> {
                        Uri.withAppendedPath(
                            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                            Uri.encode(address),
                        )
                    }
                }
            }
            .mapNotNull { uri ->
                context.contentResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
            }
            .flatMap { cursor -> cursor.asFlowable() }
            .firstOrError()
            .map { cursor -> cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID)) }
            .map { id -> Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id) }
    }

    override fun getContacts(): RealmResults<Contact> {
        val realm = Realm.getDefaultInstance()
        return realm.where(Contact::class.java).sort("name").findAll()
    }

    override fun getUnmanagedContact(lookupKey: String): Contact? {
        return Realm.getDefaultInstance().use { realm ->
            realm
                .where(Contact::class.java)
                .equalTo("lookupKey", lookupKey)
                .findFirst()
                ?.let(realm::copyFromRealm)
        }
    }

    override fun getUnmanagedAllContacts(): List<Contact> {
        val realm = Realm.getDefaultInstance()

        return realm.where(Contact::class.java).findAll().map { realm.copyFromRealm(it) }
    }

    override fun getUnmanagedContacts(starred: Boolean): Observable<List<Contact>> {
        val realm = Realm.getDefaultInstance()

        val mobileOnly = prefs.mobileOnly.get()
        val mobileLabel by lazy {
            Phone.getTypeLabel(context.resources, Phone.TYPE_MOBILE, "Mobile").toString()
        }

        var query = realm.where(Contact::class.java)

        if (mobileOnly) {
            query = query.contains("numbers.type", mobileLabel)
        }

        if (starred) {
            query = query.equalTo("starred", true)
        }

        return query
            .findAllAsync()
            .asObservable()
            .filter { it.isLoaded }
            .filter { it.isValid }
            .map { realm.copyFromRealm(it) }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .map { contacts ->
                if (mobileOnly) {
                    contacts.map { contact ->
                        val filteredNumbers =
                            contact.numbers.filter { number -> number.type == mobileLabel }
                        contact.numbers.clear()
                        contact.numbers.addAll(filteredNumbers)
                        contact
                    }
                } else {
                    contacts
                }
            }
            .map { contacts ->
                contacts.sortedWith(
                    Comparator { c1, c2 ->
                        val initial = c1.name.firstOrNull()
                        val other = c2.name.firstOrNull()
                        if (initial?.isLetter() == true && other?.isLetter() != true) {
                            -1
                        } else if (initial?.isLetter() != true && other?.isLetter() == true) {
                            1
                        } else {
                            c1.name.compareTo(c2.name, ignoreCase = true)
                        }
                    }
                )
            }
    }

    override fun getUnmanagedContactGroups(): Observable<List<ContactGroup>> {
        val realm = Realm.getDefaultInstance()
        return realm
            .where(ContactGroup::class.java)
            .isNotEmpty("contacts")
            .findAllAsync()
            .asObservable()
            .filter { it.isLoaded }
            .filter { it.isValid }
            .map { realm.copyFromRealm(it) }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
    }

    override fun setDefaultPhoneNumber(lookupKey: String, phoneNumberId: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val contact =
                realm.where(Contact::class.java).equalTo("lookupKey", lookupKey).findFirst()
                    ?: return

            realm.executeTransaction {
                contact.numbers.forEach { number -> number.isDefault = number.id == phoneNumberId }
            }
        }
    }

    override fun isContact(address: String): Boolean {
        val uri: Uri
        if (address.contains('@')) {
            uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(address))
        } else {
            uri =
                Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(address),
                )
        }
        return context.contentResolver
            .query(uri, arrayOf(BaseColumns._ID), null, null, null)
            ?.count!! > 0
    }
}
