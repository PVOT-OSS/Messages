/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony.Threads
import javax.inject.Inject
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Recipient

class CursorToConversationImpl
@Inject
constructor(private val context: Context, private val permissionManager: PermissionManager) :
    CursorToConversation {

    companion object {
        val URI: Uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val PROJECTION = arrayOf(Threads._ID, Threads.RECIPIENT_IDS)

        const val ID = 0
        const val RECIPIENT_IDS = 1
    }

    override fun map(from: Cursor): Conversation {
        return Conversation().apply {
            id = from.getLong(ID)
            recipients.addAll(
                from
                    .getString(RECIPIENT_IDS)
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .map { recipientId -> recipientId.toLong() }
                    .map { recipientId -> Recipient().apply { id = recipientId } }
            )
        }
    }

    override fun getConversationsCursor(): Cursor? {
        return when (permissionManager.hasReadSms()) {
            true -> context.contentResolver.query(URI, PROJECTION, null, null, "date desc")
            false -> null
        }
    }
}
