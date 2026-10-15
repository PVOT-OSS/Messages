/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.repository

import android.net.Uri
import io.realm.RealmResults
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.ReceivedSmsDetails

interface MessageRepository {

    fun getMessages(threadId: Long, query: String = ""): RealmResults<Message>

    fun getMessagesSync(threadId: Long, query: String = ""): RealmResults<Message>

    fun getMessage(id: Long): Message?

    fun getMessageForPart(id: Long): Message?

    fun getLastIncomingMessage(threadId: Long): RealmResults<Message>

    fun getUnreadCount(): Long

    fun getPart(id: Long): MmsPart?

    fun getPartsForConversation(threadId: Long): RealmResults<MmsPart>

    fun savePart(id: Long): Uri?

    /**
     * Retrieves the list of messages which should be shown in the notification for a given
     * conversation
     */
    fun getUnreadUnseenMessages(threadId: Long): RealmResults<Message>

    /**
     * Retrieves the list of messages which should be shown in the quickreply popup for a given
     * conversation
     */
    fun getUnreadMessages(threadId: Long): RealmResults<Message>

    fun markAllSeen(): Int

    fun markSeen(threadId: Long): Int

    fun markRead(threadIds: Collection<Long>): Int

    fun markUnread(threadIds: Collection<Long>): Int

    fun sendMessage(
        subId: Int,
        threadId: Long,
        addresses: Collection<String>,
        body: String,
        attachments: Collection<Attachment>,
        delay: Int = 0,
        applySignature: Boolean = true,
    )

    /**
     * Attempts to send the SMS message. This can be called if the message has already been
     * persisted
     */
    fun sendSms(message: Message)

    fun resendMms(message: Message)

    fun downloadMms(message: Message, automatic: Boolean = false)

    fun downloadMms(notificationUri: Uri, subId: Int, automatic: Boolean)

    fun getPendingMmsDownloads(since: Long): List<Message>

    fun getUnsentSms(): List<Message>

    fun scheduleDelayedSms(id: Long, sendTime: Long)

    fun sendDelayedSmsNow(id: Long)

    fun cancelDelayedSms(id: Long): Boolean

    fun insertSentSms(
        subId: Int,
        threadId: Long,
        address: String,
        body: String,
        date: Long,
    ): Message

    fun insertReceivedSms(
        subId: Int,
        address: String,
        body: String,
        sentTime: Long,
        details: ReceivedSmsDetails = ReceivedSmsDetails(),
    ): Message

    /** Marks the message as sending, in case we need to retry sending it */
    fun markSending(id: Long)

    fun markSent(id: Long)

    fun markFailed(id: Long, resultCode: Int)

    fun markDelivered(id: Long)

    fun markDeliveryFailed(id: Long, resultCode: Int)

    fun deleteMessages(messageIds: Collection<Long>)

    /** Returns the number of messages older than [maxAgeDays] per conversation */
    fun getOldMessageCounts(maxAgeDays: Int): Map<Long, Int>

    /** Deletes all messages older than [maxAgeDays] */
    fun deleteOldMessages(maxAgeDays: Int)
}
