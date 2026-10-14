/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.util

import java.util.concurrent.atomic.AtomicInteger

fun runAfter(count: Int, action: () -> Unit): () -> Unit {
    if (count <= 0) {
        action()
        return {}
    }

    val remaining = AtomicInteger(count)
    return {
        if (remaining.decrementAndGet() == 0)
            action()
    }
}
