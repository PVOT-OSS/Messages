/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.BufferedSink
import okio.BufferedSource
import org.prauga.messages.repository.BackupRepositoryImpl.BackupMessage

/** Reads and writes the backup file one message at a time, in the same format as before */
internal class BackupJson(moshi: Moshi) {

    private val messageAdapter = moshi.adapter(BackupMessage::class.java)

    fun write(sink: BufferedSink, messageCount: Int, messages: ((BackupMessage) -> Unit) -> Unit) {
        val writer = JsonWriter.of(sink).apply { indent = "\t" }
        writer.beginObject()
        writer.name("messageCount").value(messageCount.toLong())
        writer.name("messages").beginArray()
        messages { message -> messageAdapter.toJson(writer, message) }
        writer.endArray()
        writer.endObject()
        writer.flush()
    }

    /**
     * Calls [onMessage] for every message in the file, stopping early when it returns false.
     * Returns how many messages were read.
     */
    fun read(source: BufferedSource, onMessage: (BackupMessage?) -> Boolean = { true }): Int {
        var count = 0
        val reader = JsonReader.of(source)
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() != "messages") {
                reader.skipValue()
                continue
            }
            reader.beginArray()
            while (reader.hasNext()) {
                count++
                if (!onMessage(messageAdapter.fromJson(reader))) return count
            }
            reader.endArray()
        }
        reader.endObject()
        if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
            throw JsonDataException("JSON document was not fully consumed.")
        }
        return count
    }
}
