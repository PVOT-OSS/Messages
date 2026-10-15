/*
 * SPDX-FileCopyrightText: 2026 Shubham-344 <shubhamanand030404@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

sealed class SearchItem {
    data class Header(val conversationId: Long, val title: String) : SearchItem()

    data class Message(
        val messageId: Long,
        val conversationId: Long,
        val body: String,
        val timestamp: Long,
    ) : SearchItem()
}
