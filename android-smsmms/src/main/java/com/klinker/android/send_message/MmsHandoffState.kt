/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

class MmsHandoffState(private val prefs: SharedPreferences) {

    enum class Outcome {
        CURRENT,
        LATE,
        STALE,
    }

    private data class Entry(val attempt: Long, val state: Int, val subId: Int)

    fun start(key: String, attempt: Long, subId: Int) =
        synchronized(LOCK) {
            prefs.all.keys
                .filter { it != key && !it.startsWith(PROBE_PREFIX) }
                .filter { (read(it)?.attempt ?: 0L) < attempt - MAX_AGE_MILLIS }
                .forEach(::remove)
            write(key, Entry(attempt, WAITING, subId))
        }

    fun isWaiting(key: String, attempt: Long): Boolean =
        synchronized(LOCK) { read(key)?.let { it.attempt == attempt && it.state == WAITING } }
            ?: false

    fun timeOut(key: String, attempt: Long, noNetwork: Boolean): Boolean =
        synchronized(LOCK) {
            val entry = read(key)?.takeIf { it.attempt == attempt && it.state == WAITING }
            entry?.let { write(key, it.copy(state = if (noNetwork) NO_NETWORK else TIMED_OUT)) }
            entry != null
        }

    fun result(key: String, attempt: Long?, timedOut: Boolean, succeeded: Boolean): Outcome =
        synchronized(LOCK) {
            val entry = read(key)
            when {
                timedOut ->
                    if (entry != null && entry.attempt == attempt && entry.state != WAITING) {
                        Outcome.CURRENT
                    } else {
                        Outcome.STALE
                    }
                attempt == null || entry == null -> {
                    remove(key)
                    Outcome.CURRENT
                }
                entry.attempt != attempt -> Outcome.STALE
                entry.state == WAITING -> {
                    remove(key)
                    Outcome.CURRENT
                }
                else -> {
                    remove(key)
                    if (succeeded && entry.state == NO_NETWORK) distrustProbe(entry.subId)
                    Outcome.LATE
                }
            }
        }

    fun probeTrusted(subId: Int): Boolean = !prefs.getBoolean(probeKey(subId), false)

    private fun distrustProbe(subId: Int) =
        prefs.edit(commit = true) { putBoolean(probeKey(subId), true) }

    private fun read(key: String): Entry? =
        prefs
            .getString(key, null)
            ?.split(',')
            ?.mapNotNull(String::toLongOrNull)
            ?.takeIf { it.size == 3 }
            ?.let { (attempt, state, subId) -> Entry(attempt, state.toInt(), subId.toInt()) }

    private fun write(key: String, entry: Entry) =
        prefs.edit(commit = true) {
            putString(key, "${entry.attempt},${entry.state},${entry.subId}")
        }

    private fun remove(key: String) = prefs.edit(commit = true) { remove(key) }

    private fun probeKey(subId: Int) = "$PROBE_PREFIX$subId"

    private companion object {
        const val WAITING = 0
        const val TIMED_OUT = 1
        const val NO_NETWORK = 2

        const val PROBE_PREFIX = "probe-off:"
        val MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(2)

        val LOCK = Any()
    }
}
