/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import javax.inject.Inject

class CursorToContactGroupMemberImpl @Inject constructor(private val context: Context) :
    CursorToContactGroupMember {

    companion object {
        private val URI = ContactsContract.Data.CONTENT_URI
        private val PROJECTION =
            arrayOf(ContactsContract.Data.LOOKUP_KEY, ContactsContract.Data.DATA1)

        private const val SELECTION = "${ContactsContract.Data.MIMETYPE}=?"
        private val SELECTION_ARGS =
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)

        private const val LOOKUP_KEY = 0
        private const val GROUP_ID = 1
    }

    override fun map(from: Cursor): CursorToContactGroupMember.GroupMember {
        return CursorToContactGroupMember.GroupMember(
            from.getString(LOOKUP_KEY),
            from.getLong(GROUP_ID),
        )
    }

    override fun getGroupMembersCursor(): Cursor? {
        return context.contentResolver.query(URI, PROJECTION, SELECTION, SELECTION_ARGS, null)
    }
}
