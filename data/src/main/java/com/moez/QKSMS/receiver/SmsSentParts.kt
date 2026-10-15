/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

class SmsSentParts {

    enum class Outcome {
        NONE,
        SENT,
        FAILED,
        FAILED_SILENTLY,
    }

    private val failed = HashSet<Pair<Long, Long>>()

    @Synchronized
    fun onResult(id: Long, attempt: Long, part: Int, parts: Int, ok: Boolean): Outcome {
        val key = id to attempt
        val last = part >= parts - 1

        val outcome =
            when {
                !ok && failed.add(key) -> Outcome.FAILED
                !ok -> Outcome.FAILED_SILENTLY
                last && key !in failed -> Outcome.SENT
                else -> Outcome.NONE
            }

        if (last) failed.remove(key)
        return outcome
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_ATTEMPT = "attempt"
        const val EXTRA_PART = "part"
        const val EXTRA_PARTS = "parts"
    }
}
