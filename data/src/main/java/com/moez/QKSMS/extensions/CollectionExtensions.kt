/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

inline fun <K, T> Iterable<T>.associateByNotNull(keySelector: (T) -> K?): Map<K, T> {
    val map = hashMapOf<K, T>()
    forEach { value ->
        val key = keySelector(value)
        if (key != null) {
            map[key] = value
        }
    }

    return map
}
