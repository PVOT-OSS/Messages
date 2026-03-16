/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.model

sealed class SearchItem {
    data class Header(
        val conversationId: Long,
        val title: String
    ) : SearchItem()

    data class Message(
        val messageId: Long,
        val conversationId: Long,
        val body: String,
        val timestamp: Long
    ) : SearchItem()
}
