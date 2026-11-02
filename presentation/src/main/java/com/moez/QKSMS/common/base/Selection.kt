/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

/**
 * What is selected in a list, without listing every id: each item up to [upToId] except [except],
 * plus [picked]
 */
data class Selection(
    val upToId: Long = 0,
    val except: Set<Long> = emptySet(),
    val picked: Set<Long> = emptySet(),
    val count: Int = 0,
) {
    operator fun contains(id: Long) = if (id <= upToId) id !in except else id in picked
}
