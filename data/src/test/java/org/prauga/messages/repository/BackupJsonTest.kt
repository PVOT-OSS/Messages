/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import okio.ForwardingSink
import okio.Source
import okio.Timeout
import okio.blackholeSink
import okio.buffer
import org.junit.Test
import org.prauga.messages.repository.BackupRepositoryImpl.BackupMessage

class BackupJsonTest {

    /** The shape the whole backup used to be written and read with */
    data class Backup(val messageCount: Int = 0, val messages: List<BackupMessage> = listOf())

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val json = BackupJson(moshi)

    private fun message(i: Int, body: String = "message $i") =
        BackupMessage(
            type = 1 + i % 2,
            address = "+1555${i.toString().padStart(7, '0')}",
            date = 1_700_000_000_000L + i,
            dateSent = 1_700_000_000_000L + i - 5,
            read = i % 3 == 0,
            status = -1,
            body = body,
            protocol = 0,
            serviceCenter = if (i % 2 == 0) null else "+15550000",
            locked = i % 5 == 0,
            subId = i % 2,
        )

    private fun written(messages: List<BackupMessage>): String {
        val buffer = Buffer()
        json.write(buffer, messages.size) { write -> messages.forEach(write) }
        return buffer.readUtf8()
    }

    @Test
    fun givenMessages_whenWritten_thenFileIsByteForByteTheOldFormat() {
        val messages =
            listOf(
                message(0),
                message(1, "quote \" backslash \\ newline \n tab \t"),
                message(2, "हिन्दी 中文 😀  "),
                message(3, ""),
            )
        val old = moshi.adapter(Backup::class.java).indent("\t")

        assertThat(written(messages)).isEqualTo(old.toJson(Backup(messages.size, messages)))
        assertThat(written(emptyList())).isEqualTo(old.toJson(Backup(0, emptyList())))
    }

    @Test
    fun givenAnOldBackup_whenRead_thenEveryMessageComesBackInOrder() {
        val messages = (0 until 50).map { message(it) }
        val file = moshi.adapter(Backup::class.java).indent("\t").toJson(Backup(50, messages))
        val read = mutableListOf<BackupMessage?>()

        val count =
            json.read(Buffer().writeUtf8(file)) {
                read += it
                true
            }

        assertThat(count).isEqualTo(50)
        assertThat(read).containsExactlyElementsIn(messages).inOrder()
    }

    @Test
    fun givenAHugeBackup_whenRead_thenMessagesArriveBeforeTheFileIsLoaded() {
        val source = GeneratedBackup(100_000) { message(it) }
        var bytesWhenFirstArrived = -1L

        val count =
            json.read(source.buffer()) {
                if (bytesWhenFirstArrived < 0) bytesWhenFirstArrived = source.produced
                true
            }

        assertThat(count).isEqualTo(100_000)
        assertThat(bytesWhenFirstArrived).isLessThan(64 * 1024L)
        assertThat(source.produced).isGreaterThan(10_000_000L)
    }

    @Test
    fun givenAHugeBackup_whenWritten_thenBytesLeaveBeforeTheLastMessage() {
        var flushed = 0L
        val sink =
            object : ForwardingSink(blackholeSink()) {
                override fun write(source: Buffer, byteCount: Long) {
                    flushed += byteCount
                    super.write(source, byteCount)
                }
            }
        var flushedAtLast = -1L

        sink.buffer().use { buffered ->
            json.write(buffered, 100_000) { write ->
                repeat(100_000) {
                    if (it == 99_999) flushedAtLast = flushed
                    write(message(it))
                }
            }
        }

        assertThat(flushedAtLast).isGreaterThan(10_000_000L)
        assertThat(flushed - flushedAtLast).isLessThan(64 * 1024L)
    }

    @Test
    fun givenAStop_whenReading_thenNoMoreMessagesAreRead() {
        val file = written((0 until 10).map { message(it) })
        var seen = 0

        val count = json.read(Buffer().writeUtf8(file)) { ++seen < 3 }

        assertThat(count).isEqualTo(3)
        assertThat(seen).isEqualTo(3)
    }

    @Test
    fun givenACorruptBackup_whenRead_thenItFailsLikeBefore() {
        val file = written((0 until 10).map { message(it) })
        val old = moshi.adapter(Backup::class.java)

        listOf(file.dropLast(40), "$file {}", file.replace("\"date\"", "\"date\": \"x\", \"y\""))
            .forEach { corrupt ->
                val before = runCatching { old.fromJson(corrupt) }.exceptionOrNull()!!
                val now = runCatching { json.read(Buffer().writeUtf8(corrupt)) }.exceptionOrNull()
                assertThat(now).isInstanceOf(before.javaClass)
            }
    }

    /** Produces a backup file on the fly, so the test never holds it whole */
    private inner class GeneratedBackup(
        private val count: Int,
        private val message: (Int) -> BackupMessage,
    ) : Source {
        private val adapter = moshi.adapter(BackupMessage::class.java)
        private val pending = Buffer().writeUtf8("{\"messageCount\":$count,\"messages\":[")
        private var next = 0
        private var closed = false
        var produced = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            while (pending.size < byteCount && !closed) {
                when {
                    next < count -> {
                        if (next > 0) pending.writeUtf8(",")
                        pending.writeUtf8(adapter.toJson(message(next++)))
                    }
                    else -> {
                        pending.writeUtf8("]}")
                        closed = true
                    }
                }
            }
            if (pending.size == 0L) return -1
            val read = pending.read(sink, byteCount)
            produced += read
            return read
        }

        override fun timeout() = Timeout.NONE

        override fun close() = Unit
    }
}
