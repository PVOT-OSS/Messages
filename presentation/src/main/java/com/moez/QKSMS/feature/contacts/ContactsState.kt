/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.contacts

import org.prauga.messages.feature.compose.editing.ComposeItem
import org.prauga.messages.model.Contact

data class ContactsState(
    val query: String = "",
    val composeItems: List<ComposeItem> = ArrayList(),
    val selectedContact: Contact? = null, // For phone number picker
)
