/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class Conversation(
    @PrimaryKey var id: Long = 0,
    @Index var archived: Boolean = false,
    @Index var blocked: Boolean = false,
    @Index var pinned: Boolean = false,
    var recipients: RealmList<Recipient> = RealmList(),
    var lastMessage: Message? = null,
    var draft: String = "",
    var draftDate: Long = 0,
    var blockingClient: Int? = null,
    var blockReason: String? = null,
    var name: String =
        "", // For group chats, the user is allowed to set a custom title for the conversation
) : RealmObject() {

    val date: Long
        get() = lastMessage?.date ?: if (draft.isNotEmpty()) draftDate else 0

    val snippet: String?
        get() = lastMessage?.getSummary()

    val unread: Boolean
        get() = lastMessage?.read == false

    val me: Boolean
        get() = lastMessage?.isMe() == true

    fun getTitle(): String {
        return name.takeIf { it.isNotBlank() }
            ?: recipients.joinToString { recipient -> recipient.getDisplayName() }
    }
}
