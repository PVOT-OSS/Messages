/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.database.Cursor

interface CursorToContactGroupMember : Mapper<Cursor, CursorToContactGroupMember.GroupMember> {

    data class GroupMember(val lookupKey: String, val groupId: Long)

    fun getGroupMembersCursor(): Cursor?
}
