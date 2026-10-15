/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.filter

import javax.inject.Inject
import org.prauga.messages.extensions.fuzzyMatch
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.Contact

class ContactFilter @Inject constructor(private val phoneNumberFilter: PhoneNumberFilter) :
    Filter<Contact>() {

    override fun filter(item: Contact, query: CharSequence): Boolean {
        val normalizedName = item.name.removeAccents()
        return normalizedName.contains(query, true) || // exact/substring
            normalizedName.fuzzyMatch(query) || // fuzzy
            item.numbers
                .map { it.address }
                .any { address -> phoneNumberFilter.filter(address, query) } // Number
    }
}
