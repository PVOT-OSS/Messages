/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.database.Cursor
import org.prauga.messages.model.Recipient

interface CursorToRecipient : Mapper<Cursor, Recipient> {

    fun getRecipientCursor(): Cursor?

    fun getRecipientCursor(id: Long): Cursor?
}
