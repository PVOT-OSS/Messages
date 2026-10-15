/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import javax.inject.Inject
import org.prauga.messages.model.MmsPart

class CursorToPartImpl @Inject constructor(private val context: Context) : CursorToPart {

    companion object {
        val CONTENT_URI: Uri = Uri.parse("content://mms/part")
    }

    override fun map(from: Cursor) =
        MmsPart().apply {
            id = from.getLong(from.getColumnIndexOrThrow(Telephony.Mms.Part._ID))
            messageId = from.getLong(from.getColumnIndexOrThrow(Telephony.Mms.Part.MSG_ID))
            type =
                from.getStringOrNull(from.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE))
                    ?: "*/*"
            seq = from.getIntOrNull(from.getColumnIndexOrThrow(Telephony.Mms.Part.SEQ)) ?: -1
            name =
                from.getStringOrNull(from.getColumnIndexOrThrow(Telephony.Mms.Part.NAME))
                    ?: from
                        .getStringOrNull(
                            from.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_LOCATION)
                        )
                        ?.split("/")
                        ?.last()
            text = from.getStringOrNull(from.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT))
        }

    override fun getPartsCursor(messageId: Long?): Cursor? {
        return when (messageId) {
            null -> context.contentResolver.query(CONTENT_URI, null, null, null, null)
            else ->
                context.contentResolver.query(
                    CONTENT_URI,
                    null,
                    "${Telephony.Mms.Part.MSG_ID} = ?",
                    arrayOf(messageId.toString()),
                    null,
                )
        }
    }
}
