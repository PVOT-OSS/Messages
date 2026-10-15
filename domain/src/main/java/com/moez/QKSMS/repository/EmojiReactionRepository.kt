/*
 * SPDX-FileCopyrightText: 2025 Ben Iofel <ben@iofel.me>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
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

    fun deleteAndReparseAllEmojiReactions(realm: Realm)
}
