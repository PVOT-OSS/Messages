/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.filter

import javax.inject.Inject
import org.prauga.messages.model.Recipient

class RecipientFilter
@Inject
constructor(
    private val contactFilter: ContactFilter,
    private val phoneNumberFilter: PhoneNumberFilter,
) : Filter<Recipient>() {

    override fun filter(item: Recipient, query: CharSequence) =
        when {
            item.contact?.let { contactFilter.filter(it, query) } == true -> true
            phoneNumberFilter.filter(item.address, query) -> true
            else -> false
        }
}
