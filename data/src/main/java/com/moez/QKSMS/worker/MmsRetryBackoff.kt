/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MmsRetryBackoff(context: Context, name: String, private val window: Long = RETRY_WINDOW) {

    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun nextDelay(key: String, now: Long = System.currentTimeMillis()): Long? {
        val (attempt, firstFailure) =
            prefs
                .getString(key, null)
                ?.split(',')
                ?.mapNotNull(String::toLongOrNull)
                ?.takeIf { it.size == 2 }
                ?.let { (attempt, firstFailure) -> attempt.toInt() to firstFailure } ?: (0 to now)

        if (now - firstFailure > window) {
            clear(key)
            return null
        }

        prefs.edit { putString(key, "${attempt + 1},$firstFailure") }
        return retryDelay(attempt)
    }

    fun reset(key: String) {
        val firstFailure =
            prefs.getString(key, null)?.substringAfter(',', "")?.toLongOrNull() ?: return
        prefs.edit { putString(key, "0,$firstFailure") }
    }

    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    fun keys(): Set<String> = prefs.all.keys

    companion object {
        private val MIN_DELAY = TimeUnit.SECONDS.toMillis(5)
        private val MAX_DELAY = TimeUnit.MINUTES.toMillis(20)
        private val RETRY_WINDOW = TimeUnit.HOURS.toMillis(12)

        val DOWNLOAD_WINDOW = TimeUnit.MINUTES.toMillis(20)

        fun retryDelay(attempt: Int, random: Random = Random.Default): Long {
            val delay = (MIN_DELAY shl attempt.coerceIn(0, 20)).coerceAtMost(MAX_DELAY)
            return (delay / 2 + random.nextLong(delay / 2 + 1)).coerceAtLeast(MIN_DELAY)
        }
    }
}
