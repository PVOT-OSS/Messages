/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

class SmsSentParts(private val store: Store) {

    interface Store {
        fun get(key: String): String?

        fun put(key: String, value: String)

        fun remove(key: String)
    }

    enum class Category {
        SENT,
        AUTO_RETRY,
        MANUAL_RETRY,
    }

    data class Result(val category: Category, val resultCode: Int, val errorCode: Int)

    @Synchronized
    fun onResult(id: Long, attempt: Long, parts: Int, result: Result): Result? {
        if (parts <= 1) return result

        val key = "$id:$attempt"
        val state = store.get(key)?.let(::decode)
        if (state == null) {
            store.put(key, encode(parts - 1 to result))
            return null
        }

        val (remaining, worst) = state
        val combined = if (result.category > worst.category) result else worst
        if (remaining <= 1) {
            store.remove(key)
            return combined
        }

        store.put(key, encode(remaining - 1 to combined))
        return null
    }

    private fun encode(state: Pair<Int, Result>): String {
        val (remaining, result) = state
        return "$remaining,${result.category.ordinal},${result.resultCode},${result.errorCode}"
    }

    private fun decode(value: String): Pair<Int, Result>? {
        val fields = value.split(',').mapNotNull(String::toIntOrNull).takeIf { it.size == 4 }
        return fields?.let { (remaining, category, resultCode, errorCode) ->
            val result = Category.entries.getOrNull(category) ?: return null
            remaining to Result(result, resultCode, errorCode)
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_ATTEMPT = "attempt"
        const val EXTRA_PART = "part"
        const val EXTRA_PARTS = "parts"
    }
}
