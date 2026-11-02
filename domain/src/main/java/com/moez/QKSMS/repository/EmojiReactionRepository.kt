/*
 * SPDX-FileCopyrightText: 2025 Ben Iofel <ben@iofel.me>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import io.realm.Realm
import org.prauga.messages.model.Message

data class ParsedEmojiReaction(
    val emoji: String,
    val originalMessage: String,
    val isRemoval: Boolean = false,
)

interface EmojiReactionRepository {
    fun parseEmojiReaction(body: String): ParsedEmojiReaction?

    /** Builds an outgoing reaction/tapback body that matches iOS-style SMS fallback formatting. */
    fun buildOutgoingReactionBody(
        emoji: String,
        targetMessageText: String,
        isRemoval: Boolean = false,
    ): String?

    fun findTargetMessage(threadId: Long, originalMessageText: String, realm: Realm): Message?

    fun saveEmojiReaction(
        reactionMessage: Message,
        parsedReaction: ParsedEmojiReaction,
        targetMessage: Message?,
        realm: Realm,
    )

    /** Runs its own transactions, so call it outside of one. */
    fun deleteAndReparseAllEmojiReactions(realm: Realm)

    /** Reparses every reaction if an upgrade asked for it, on the calling thread. */
    fun reparseIfNeeded()
}
