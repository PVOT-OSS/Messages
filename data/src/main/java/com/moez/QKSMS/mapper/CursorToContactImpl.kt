/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Phone
import javax.inject.Inject
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.model.Contact
import org.prauga.messages.model.PhoneNumber

class CursorToContactImpl
@Inject
constructor(private val context: Context, private val permissionManager: PermissionManager) :
    CursorToContact {

    companion object {
        val URI = Phone.CONTENT_URI
        val PROJECTION =
            arrayOf(
                Phone._ID,
                Phone.LOOKUP_KEY,
                Phone.ACCOUNT_TYPE_AND_DATA_SET,
                Phone.NUMBER,
                Phone.TYPE,
                Phone.LABEL,
                Phone.DISPLAY_NAME,
                Phone.PHOTO_URI,
                Phone.STARRED,
                Phone.CONTACT_LAST_UPDATED_TIMESTAMP,
            )

        const val COLUMN_ID = 0
        const val COLUMN_LOOKUP_KEY = 1
        const val COLUMN_ACCOUNT_TYPE = 2
        const val COLUMN_NUMBER = 3
        const val COLUMN_TYPE = 4
        const val COLUMN_LABEL = 5
        const val COLUMN_DISPLAY_NAME = 6
        const val COLUMN_PHOTO_URI = 7
        const val COLUMN_STARRED = 8
        const val CONTACT_LAST_UPDATED = 9
    }

    override fun map(from: Cursor) =
        Contact().apply {
            lookupKey = from.getString(COLUMN_LOOKUP_KEY)
            name = from.getString(COLUMN_DISPLAY_NAME) ?: ""
            photoUri = from.getString(COLUMN_PHOTO_URI)
            numbers.add(
                PhoneNumber(
                    id = from.getLong(COLUMN_ID),
                    accountType = from.getString(COLUMN_ACCOUNT_TYPE),
                    address = from.getString(COLUMN_NUMBER) ?: "",
                    type =
                        Phone.getTypeLabel(
                                context.resources,
                                from.getInt(COLUMN_TYPE),
                                from.getString(COLUMN_LABEL),
                            )
                            .toString(),
                )
            )
            starred = from.getInt(COLUMN_STARRED) != 0
            lastUpdate = from.getLong(CONTACT_LAST_UPDATED)
        }

    override fun getContactsCursor(): Cursor? {
        return when (permissionManager.hasContacts()) {
            true -> context.contentResolver.query(URI, PROJECTION, null, null, null)
            false -> null
        }
    }
}
