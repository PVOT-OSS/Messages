/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.worker

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MmsRetryBackoff(context: Context, name: String) {

    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun nextDelay(key: String, now: Long = System.currentTimeMillis()): Long? {
        val (attempt, firstFailure) =
            prefs.getString(key, null)
                ?.split(',')
                ?.mapNotNull(String::toLongOrNull)
                ?.takeIf { it.size == 2 }
                ?.let { (attempt, firstFailure) -> attempt.toInt() to firstFailure }
                ?: (0 to now)

        if (now - firstFailure > RETRY_WINDOW) {
            clear(key)
            return null
        }

        prefs.edit { putString(key, "${attempt + 1},$firstFailure") }
        return retryDelay(attempt)
    }

    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    companion object {
        private val MIN_DELAY = TimeUnit.SECONDS.toMillis(5)
        private val MAX_DELAY = TimeUnit.MINUTES.toMillis(20)
        private val RETRY_WINDOW = TimeUnit.HOURS.toMillis(12)

        fun retryDelay(attempt: Int, random: Random = Random.Default): Long {
            val delay = (MIN_DELAY shl attempt.coerceIn(0, 20)).coerceAtMost(MAX_DELAY)
            return (delay / 2 + random.nextLong(delay / 2 + 1)).coerceAtLeast(MIN_DELAY)
        }
    }
}
