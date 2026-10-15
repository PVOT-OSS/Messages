/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

import android.database.Cursor
import io.reactivex.Flowable

fun Cursor.forEach(closeOnComplete: Boolean = true, method: (Cursor) -> Unit = {}) {
    moveToPosition(-1)
    while (moveToNext()) {
        method.invoke(this)
    }

    if (closeOnComplete) {
        close()
    }
}

fun <T> Cursor.map(map: (Cursor) -> T): List<T> {
    return List(count) { position ->
        moveToPosition(position)
        map(this)
    }
}

/**
 * We're using this simple implementation with .range() because of the complexities of dealing with
 * Backpressure with a Cursor. We can't simply use a loop and call onNext() from a generator because
 * we'll need to close the cursor at the end, and if any items are still in the buffer, then they
 * will be made invalid
 */
fun Cursor.asFlowable(): Flowable<Cursor> {
    return Flowable.range(0, count)
        .map {
            moveToPosition(it)
            this
        }
        .doOnComplete { close() }
}

/** Dumps the contents of the cursor as a CSV string */
fun Cursor.dump(): String {
    val lines = mutableListOf<String>()

    lines += columnNames.joinToString(",")
    forEach { lines += (0 until columnCount).joinToString(",", transform = ::getString) }

    return lines.joinToString("\n")
}
