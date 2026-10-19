/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.mapper

import android.database.Cursor
import org.prauga.messages.model.MmsPart

interface CursorToPart : Mapper<Cursor, MmsPart> {

    fun getPartsCursor(messageId: Long? = null): Cursor?
}
