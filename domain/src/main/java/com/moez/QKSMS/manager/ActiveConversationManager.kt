/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

/**
 * Keeps track of the conversation that the user is currently viewing. This is useful when we
 * receive a message, because it allows us to immediately mark the message read and not display a
 * notification
 */
interface ActiveConversationManager {

    fun setActiveConversation(threadId: Long?)

    fun getActiveConversation(): Long?
}
