/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import timber.log.Timber

/** Utility class to make ContentResolver queries with Kotlin more concise */
object SqliteWrapper {

    fun query(
        context: Context,
        uri: Uri,
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null,
        logError: Boolean = true,
    ): Cursor? {
        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        } catch (e: SQLiteException) {
            if (logError) {
                Timber.e(e)
            }
            null
        }
    }
}
