/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util.extensions

import java.util.Calendar

fun Calendar.isSameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

fun Calendar.isSameWeek(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.WEEK_OF_YEAR) == other.get(Calendar.WEEK_OF_YEAR)
}

fun Calendar.isSameYear(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR)
}
