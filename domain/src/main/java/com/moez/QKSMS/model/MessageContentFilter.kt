/*
 * SPDX-FileCopyrightText: 2025 peanut250 <209579534+peanut250@users.noreply.github.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class MessageContentFilter(
    @PrimaryKey var id: Long = 0,
    var value: String = "",
    var caseSensitive: Boolean = false,
    var isRegex: Boolean = false,
    var includeContacts: Boolean = false,
) : RealmObject()

data class MessageContentFilterData(
    var value: String = "",
    var caseSensitive: Boolean = false,
    var isRegex: Boolean = false,
    var includeContacts: Boolean = false,
)
