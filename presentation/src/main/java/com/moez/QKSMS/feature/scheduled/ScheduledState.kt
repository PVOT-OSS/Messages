/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.scheduled

import io.realm.RealmResults
import org.prauga.messages.model.ScheduledMessage

data class ScheduledState(
    val scheduledMessages: RealmResults<ScheduledMessage>? = null,
    val upgraded: Boolean = true,
    val selectedMessages: Int = 0,
    val conversationId: Long? = null,
)
