/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.contacts

import io.reactivex.Observable
import io.reactivex.subjects.Subject
import org.prauga.messages.common.base.QkView
import org.prauga.messages.extensions.Optional
import org.prauga.messages.feature.compose.editing.ComposeItem
import org.prauga.messages.feature.compose.editing.PhoneNumberAction

interface ContactsContract : QkView<ContactsState> {

    val queryChangedIntent: Observable<CharSequence>
    val queryClearedIntent: Observable<*>
    val queryEditorActionIntent: Observable<Int>
    val composeItemPressedIntent: Subject<ComposeItem>
    val composeItemLongPressedIntent: Subject<ComposeItem>
    val phoneNumberSelectedIntent: Subject<Optional<Long>>
    val phoneNumberActionIntent: Subject<PhoneNumberAction>

    fun clearQuery()

    fun openKeyboard()

    fun finish(result: HashMap<String, String?>)
}
