/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.extensions

import java.text.Normalizer

/**
 * Strip the accents from a string
 */
fun CharSequence.removeAccents(): String =
    Normalizer.normalize(this, Normalizer.Form.NFKD).replace(Regex("\\p{M}"), "")

fun String.joinTo(rhs: String, separator: String) =
    when {
        this.isEmpty() -> rhs
        rhs.isEmpty() -> this
        else -> "$this$separator$rhs"
    }

fun String.truncateWithEllipses(maxLengthIncEllipses: Int) =
    when (this.length) {
        in 0..maxLengthIncEllipses -> this
        else -> this.take(maxLengthIncEllipses - 1) + "…"
    }

/**
 * Basic Levenshtein-distance fuzzy match with a cut-off to avoid expensive work.
 * Returns true if the distance is within [maxDistance].
 */
fun CharSequence.fuzzyMatch(other: CharSequence, maxDistance: Int = 2): Boolean {
    val lhs = this.toString().lowercase()
    val rhs = other.toString().lowercase()

    if (lhs == rhs) return true
    if (kotlin.math.abs(lhs.length - rhs.length) > maxDistance) return false
    if (lhs.isEmpty() || rhs.isEmpty()) return false

    val prev = IntArray(rhs.length + 1) { it }
    val curr = IntArray(rhs.length + 1)

    for (i in lhs.indices) {
        curr[0] = i + 1
        var bestInRow = curr[0]
        for (j in rhs.indices) {
            val cost = if (lhs[i] == rhs[j]) 0 else 1
            curr[j + 1] = minOf(
                prev[j + 1] + 1, // deletion
                curr[j] + 1,     // insertion
                prev[j] + cost   // substitution
            )
            bestInRow = minOf(bestInRow, curr[j + 1])
        }
        if (bestInRow > maxDistance) return false
        System.arraycopy(curr, 0, prev, 0, curr.size)
    }

    return prev[rhs.length] <= maxDistance
}
