/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import android.telephony.PhoneNumberUtils
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.Locale

open class Recipient(
    @PrimaryKey var id: Long = 0,
    var address: String = "",
    var contact: Contact? = null,
    var lastUpdate: Long = 0,
) : RealmObject() {

    /** Return a string that can be displayed to represent the name of this contact */
    fun getDisplayName(): String =
        contact?.name?.takeIf { it.isNotBlank() }
            ?: PhoneNumberUtils.formatNumber(
                address,
                Locale.getDefault().country,
            ) // TODO: Use our own PhoneNumberUtils
            ?: address
}
