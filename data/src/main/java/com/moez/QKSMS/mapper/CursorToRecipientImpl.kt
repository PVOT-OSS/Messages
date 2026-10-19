/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import javax.inject.Inject
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.model.Recipient

class CursorToRecipientImpl
@Inject
constructor(private val context: Context, private val permissionManager: PermissionManager) :
    CursorToRecipient {

    companion object {
        val URI = Uri.parse("content://mms-sms/canonical-addresses")

        const val COLUMN_ID = 0
        const val COLUMN_ADDRESS = 1
    }

    override fun map(from: Cursor) =
        Recipient(
            id = from.getLong(COLUMN_ID),
            address = from.getString(COLUMN_ADDRESS),
            lastUpdate = System.currentTimeMillis(),
        )

    override fun getRecipientCursor(): Cursor? {
        return when (permissionManager.hasReadSms()) {
            true -> context.contentResolver.query(URI, null, null, null, null)
            false -> null
        }
    }

    override fun getRecipientCursor(id: Long): Cursor? {
        return context.contentResolver.query(URI, null, "_id = ?", arrayOf(id.toString()), null)
    }
}
