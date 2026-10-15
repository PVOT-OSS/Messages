/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.listener

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

/** Listens for a contact being added, and then syncs it to Realm */
class ContactAddedListenerImpl @Inject constructor(private val context: Context) :
    ContactAddedListener {

    companion object {
        private val URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    }

    override fun listen(): Observable<*> {
        return ContactContentObserver(context).observable
    }

    private class ContactContentObserver(context: Context) : ContentObserver(Handler()) {

        private val subject = BehaviorSubject.createDefault<Unit>(Unit)

        val observable: Observable<Unit> =
            subject
                .doOnSubscribe { context.contentResolver.registerContentObserver(URI, true, this) }
                .doOnDispose { context.contentResolver.unregisterContentObserver(this) }
                .share()

        override fun onChange(selfChange: Boolean) {
            this.onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            subject.onNext(Unit)
        }
    }
}
