/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

/**
 * Messages selected in a conversation, matching [query] like the list shown: every one up to
 * [upToId] except [except], plus [picked]. Its size depends only on what was tapped.
 */
data class MessageSelection(
    val threadId: Long,
    val query: String = "",
    val upToId: Long = 0,
    val except: Set<Long> = emptySet(),
    val picked: Set<Long> = emptySet(),
)
