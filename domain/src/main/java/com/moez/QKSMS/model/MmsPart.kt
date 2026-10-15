/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.io.File

open class MmsPart : RealmObject() {

    @PrimaryKey var id: Long = 0
    @Index var messageId: Long = 0
    var type: String = ""
    var seq: Int = -1
    var name: String? = null
    var text: String? = null

    @LinkingObjects("parts") val messages: RealmResults<Message>? = null

    fun getUri(): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("mms")
            .encodedPath("part/$id")
            .build()

    fun getBestFilename(): String =
        if (File(name).extension.isNotEmpty()) name ?: "unknown"
        else
            "$name." +
                (MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                    ?: type.substringAfter("/"))

    fun getSummary(): String? =
        when {
            type == "application/smil" -> null
            type == "text/plain" -> text
            type == "text/x-vcard" -> "Contact card"
            type.startsWith("image") -> "Picture"
            type.startsWith("video") -> "Video"
            type.startsWith("audio") -> "Audio"
            else -> type.substring(type.indexOf('/') + 1)
        }
}
