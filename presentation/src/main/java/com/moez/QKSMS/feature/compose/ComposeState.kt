/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose

import io.realm.RealmResults
import org.prauga.messages.compat.SubscriptionInfoCompat
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.Recipient

data class ComposeState(
    val hasError: Boolean = false,
    val editingMode: Boolean = false,
    val threadId: Long = 0,
    val selectedChips: List<Recipient> = ArrayList(),
    val sendAsGroup: Boolean = true,
    val conversationtitle: String = "",
    val loading: Boolean = false,
    val query: String = "",
    val searchSelectionId: Long = -1,
    val searchSelectionPosition: Int = 0,
    val searchResults: Int = 0,
    val messages: Pair<Conversation, RealmResults<Message>>? = null,
    val selectedMessages: Int = 0,
    val selectedMessagesHaveText: Boolean = false,
    val selectedMessagesCanReact: Boolean = false,
    val scheduled: Long = 0,
    val attachments: List<Attachment> = listOf(),
    val attaching: Boolean = false,
    val scheduling: Boolean = false,
    val remaining: String = "",
    val subscription: SubscriptionInfoCompat? = null,
    val canSend: Boolean = false,
    val hasScheduledMessages: Boolean = false,
    val validRecipientNumbers: Int = 1,
    val audioMsgRecording: Boolean = false,
    val saveDraft: Boolean = true,
)
