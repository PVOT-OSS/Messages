/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.filter

import org.prauga.messages.extensions.fuzzyMatch
import org.prauga.messages.extensions.removeAccents
import org.prauga.messages.model.Conversation
import javax.inject.Inject

class ConversationFilter @Inject constructor(private val recipientFilter: RecipientFilter) :
    Filter<Conversation>() {

    override fun filter(item: Conversation, query: CharSequence): Boolean {
        val normalizedName = item.name.removeAccents()
        if (normalizedName.contains(query, ignoreCase = true) || normalizedName.fuzzyMatch(query)) {
            return true
        }
        val normalizedSnippet = item.snippet?.removeAccents().orEmpty()
        if (normalizedSnippet.isNotEmpty() &&
            (normalizedSnippet.contains(query, ignoreCase = true) || normalizedSnippet.fuzzyMatch(
                query
            ))
        ) {
            return true
        }
        return item.recipients.any { recipient -> recipientFilter.filter(recipient, query) }
    }
}
