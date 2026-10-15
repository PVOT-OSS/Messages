/*
 * SPDX-FileCopyrightText: 2025 peanut250 <209579534+peanut250@users.noreply.github.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import io.realm.RealmResults
import org.prauga.messages.model.MessageContentFilter
import org.prauga.messages.model.MessageContentFilterData

interface MessageContentFilterRepository {

    fun createFilter(data: MessageContentFilterData)

    fun getMessageContentFilters(): RealmResults<MessageContentFilter>

    fun getMessageContentFilter(id: Long): MessageContentFilter?

    fun isBlocked(messageBody: String, address: String, contactsRepo: ContactRepository): Boolean

    fun removeFilter(id: Long)
}
