/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo

data class ConversationInfoState(
    val threadId: Long = 0,
    val data: List<ConversationInfoItem> = listOf(),
    val hasError: Boolean = false,
)
