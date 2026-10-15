/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import java.util.concurrent.atomic.AtomicInteger

/**
 * Returns a callback that runs [action] once it has been invoked [count] times. Useful for cleaning
 * up after several async operations that share a resource.
 */
fun runAfter(count: Int, action: () -> Unit): () -> Unit {
    if (count <= 0) {
        action()
        return {}
    }

    val remaining = AtomicInteger(count)
    return { if (remaining.decrementAndGet() == 0) action() }
}
