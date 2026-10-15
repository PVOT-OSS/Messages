/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.filter

import javax.inject.Inject
import org.prauga.messages.util.PhoneNumberUtils

class PhoneNumberFilter @Inject constructor(private val phoneNumberUtils: PhoneNumberUtils) :
    Filter<String>() {

    override fun filter(item: String, query: CharSequence): Boolean {
        val allCharactersDialable = query.all { phoneNumberUtils.isReallyDialable(it) }
        return allCharactersDialable &&
            (phoneNumberUtils.compare(item, query.toString()) ||
                phoneNumberUtils
                    .normalizeNumber(item)
                    .contains(phoneNumberUtils.normalizeNumber(query.toString())))
    }
}
