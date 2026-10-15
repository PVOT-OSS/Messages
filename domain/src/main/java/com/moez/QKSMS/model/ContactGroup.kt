/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ContactGroup(
    @PrimaryKey var id: Long = 0,
    var title: String = "",
    var contacts: RealmList<Contact> = RealmList(),
) : RealmObject()
