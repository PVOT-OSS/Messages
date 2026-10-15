/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.filter

import javax.inject.Inject
import org.prauga.messages.extensions.fuzzyMatch
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.Conversation

class ConversationFilter @Inject constructor(private val recipientFilter: RecipientFilter) :
    Filter<Conversation>() {

    override fun filter(item: Conversation, query: CharSequence): Boolean {
        val normalizedName = item.name.removeAccents()
        if (normalizedName.contains(query, ignoreCase = true) || normalizedName.fuzzyMatch(query)) {
            return true
        }
        val normalizedSnippet = item.snippet?.removeAccents().orEmpty()
        if (
            normalizedSnippet.isNotEmpty() &&
                (normalizedSnippet.contains(query, ignoreCase = true) ||
                    normalizedSnippet.fuzzyMatch(query))
        ) {
            return true
        }
        return item.recipients.any { recipient -> recipientFilter.filter(recipient, query) }
    }
}
