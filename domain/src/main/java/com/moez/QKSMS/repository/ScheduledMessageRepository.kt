/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import io.realm.RealmResults
import org.prauga.messages.model.ScheduledMessage

interface ScheduledMessageRepository {

    /** Saves a scheduled message */
    fun saveScheduledMessage(
        date: Long,
        subId: Int,
        recipients: List<String>,
        sendAsGroup: Boolean,
        body: String,
        attachments: List<String>,
        conversationId: Long,
    ): ScheduledMessage

    /** Updates scheduled messages with new uris */
    fun updateScheduledMessage(scheduledMessage: ScheduledMessage)

    /** Returns all of the scheduled messages, sorted chronologically */
    fun getScheduledMessages(): RealmResults<ScheduledMessage>

    /** Returns the scheduled message with the given [id] */
    fun getScheduledMessage(id: Long): ScheduledMessage?

    /** Returns all scheduled messages with the given [conversationId] */
    fun getScheduledMessagesForConversation(conversationId: Long): RealmResults<ScheduledMessage>

    /** Deletes the scheduled message with the given [id] */
    fun deleteScheduledMessage(id: Long)

    /** Delete multiple scheduled messages by id list */
    fun deleteScheduledMessages(ids: List<Long>)

    /** Get a list of all scheduled message ids (in scheduled date order) */
    fun getAllScheduledMessageIdsSnapshot(): List<Long>
}
