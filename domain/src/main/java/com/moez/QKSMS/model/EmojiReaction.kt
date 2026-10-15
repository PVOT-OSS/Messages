/*
 * SPDX-FileCopyrightText: 2025 Ben Iofel <ben@iofel.me>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class EmojiReaction : RealmObject() {
    @PrimaryKey var id: Long = 0

    /** The reaction message ID itself */
    @Index var reactionMessageId: Long = 0

    /** The sender's address (phone number) */
    var senderAddress: String = ""

    /** The emoji used in the reaction */
    var emoji: String = ""

    /** The original message text that was reacted to */
    var originalMessageText: String = ""

    /** Thread ID for easier querying */
    @Index var threadId: Long = 0
}
