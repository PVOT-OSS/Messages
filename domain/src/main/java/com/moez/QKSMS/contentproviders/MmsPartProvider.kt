/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.moez.QKSMS.contentproviders

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException

// the primary use case for this provider is to re-provide mms attachments but with a file name
// as the last part of the uri as opposed to the system mms provider uri which has a part id as
// it's 'filename' component. with this provider external applications can save/display using the
// original file name, if available, of the attachment when it is shared and/or saved
// it seems a needlessly complicated way to achieve the requirement, but it has to be done
class MmsPartProvider : ContentProvider() {
    companion object {
        private fun getMmsPartIdFromUri(uri: Uri): String? {
            // ensure at least 2 path segments exist
            if (uri.pathSegments.size < 2) return null

            return try {
                return uri.pathSegments[uri.pathSegments.size - 2]
            } catch (e: Exception) {
                null
            }
        }

        private fun getUnderlyingUriFromUri(uri: Uri): Uri {
            val partId = getMmsPartIdFromUri(uri) ?: return Uri.EMPTY
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("mms")
                .encodedPath("part/$partId")
                .build()
        }

        fun getUriForMmsPartId(partId: Long, partName: String): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("org.prauga.messages.mmspart")
                .encodedPath("part/$partId/$partName")
                .build()
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        return context
            ?.contentResolver
            ?.query(getUnderlyingUriFromUri(uri), projection, selection, selectionArgs, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        return context?.contentResolver?.getType(getUnderlyingUriFromUri(uri))
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return context?.contentResolver?.insert(getUnderlyingUriFromUri(uri), values)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        return context
            ?.contentResolver
            ?.update(getUnderlyingUriFromUri(uri), values, selection, selectionArgs) ?: 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return context
            ?.contentResolver
            ?.delete(getUnderlyingUriFromUri(uri), selection, selectionArgs) ?: 0
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return context?.contentResolver?.openFileDescriptor(getUnderlyingUriFromUri(uri), mode)
    }
}
