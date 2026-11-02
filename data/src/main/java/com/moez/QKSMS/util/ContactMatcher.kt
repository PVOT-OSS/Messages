/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import org.prauga.messages.model.Contact

class ContactMatcher(
    private val contacts: List<Contact>,
    private val compare: (address: String, contactNumber: String) -> Boolean,
) {

    private val candidates: Map<String, List<Pair<Int, String>>> =
        contacts
            .flatMapIndexed { index, contact ->
                contact.numbers.map { number -> key(number.address) to (index to number.address) }
            }
            .groupBy({ it.first }, { it.second })

    fun find(address: String): Contact? =
        candidates[key(address)]
            ?.firstOrNull { (_, number) -> compare(address, number) }
            ?.let { (index, _) -> contacts[index] }

    private fun key(address: String): String {
        val digits = address.filter(Char::isDigit)
        return if (digits.length >= MATCH_DIGITS) digits.takeLast(MATCH_DIGITS) else "short:$digits"
    }

    private companion object {
        const val MATCH_DIGITS = 7
    }
}
