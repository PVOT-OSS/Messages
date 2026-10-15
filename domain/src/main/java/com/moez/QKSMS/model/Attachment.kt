/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import androidx.core.view.inputmethod.InputContentInfoCompat
import org.prauga.messages.extensions.contactToVCard
import org.prauga.messages.extensions.getName
import org.prauga.messages.extensions.getResourceBytes
import org.prauga.messages.extensions.getSize
import org.prauga.messages.extensions.getType
import org.prauga.messages.extensions.isAudio
import org.prauga.messages.extensions.isContact
import org.prauga.messages.extensions.isImage
import org.prauga.messages.extensions.isVCard

@SuppressLint("Range")
class Attachment(
    context: Context,
    var uri: Uri = Uri.EMPTY,
    inputContent: InputContentInfoCompat? = null,
) {
    private var resourceBytes: ByteArray? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            uri = inputContent?.contentUri ?: uri

        // if constructed with a uri to a contact, convert uri to associated vcard uri
        if (uri.isContact(context)) uri = uri.contactToVCard(context)
    }

    fun isVCard(context: Context): Boolean = uri.isVCard(context)

    fun isAudio(context: Context): Boolean = uri.isAudio(context)

    fun isImage(context: Context): Boolean = uri.isImage(context)

    fun getType(context: Context): String = uri.getType(context)

    fun getName(context: Context): String = uri.getName(context) ?: "unknown"

    fun getSize(context: Context): Long = uri.getSize(context)

    fun hasDisplayableImage(context: Context): Boolean {
        val mimeType = getType(context)
        return (mimeType.startsWith("image/") || mimeType.startsWith("video/"))
    }

    fun getResourceBytes(context: Context): ByteArray {
        // cache resource bytes by loading first time only
        if (resourceBytes != null) return resourceBytes!!

        resourceBytes = uri.getResourceBytes(context)

        return resourceBytes!!
    }

    fun releaseResourceBytes() {
        resourceBytes = null
    }

    fun removeCacheFile(context: Context): Boolean {
        if (uri.scheme != ContentResolver.SCHEME_FILE) return false

        return try {
            val file = uri.toFile().canonicalFile
            val cacheDir = context.cacheDir.canonicalFile
            file.startsWith(cacheDir) && file != cacheDir && file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
