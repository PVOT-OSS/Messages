/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.messages

import io.realm.RealmResults
import org.prauga.messages.model.Conversation

data class BlockedMessagesState(
    val data: RealmResults<Conversation>? = null,
    val selected: Int = 0,
)
