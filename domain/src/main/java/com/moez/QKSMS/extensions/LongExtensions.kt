/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

import java.util.concurrent.TimeUnit

fun Long.millisecondsToMinutes(): Long {
    return TimeUnit.MILLISECONDS.toMinutes(this)
}
