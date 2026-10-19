/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.prauga.messages.common.util.extensions.isSameDay
import org.prauga.messages.common.util.extensions.isSameWeek
import org.prauga.messages.common.util.extensions.isSameYear

@Singleton
class DateFormatter @Inject constructor(val context: Context) {

    /**
     * Formats the [pattern] correctly for the current locale, and replaces 12 hour format with 24
     * hour format if necessary
     */
    private fun getFormatter(pattern: String): SimpleDateFormat {
        var formattedPattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)

        if (DateFormat.is24HourFormat(context)) {
            formattedPattern =
                formattedPattern
                    .replace("h", "HH")
                    .replace("K", "HH")
                    .replace("\\s+a".toRegex(), "")
        }

        return SimpleDateFormat(formattedPattern, Locale.getDefault())
    }

    fun getDetailedTimestamp(date: Long): String {
        return getFormatter("M/d/y, h:mm:ss a").format(date)
    }

    fun getTimestamp(date: Long): String {
        return getFormatter("h:mm a").format(date)
    }

    fun getMessageTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameWeek(then) -> getFormatter("E h:mm a")
            now.isSameYear(then) -> getFormatter("MMM d, h:mm a")
            else -> getFormatter("MMM d yyyy, h:mm a")
        }.format(date)
    }

    fun getConversationTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameWeek(then) -> getFormatter("E")
            now.isSameYear(then) -> getFormatter("MMM d")
            else -> getFormatter("MM/d/yy")
        }.format(date)
    }

    fun getScheduledTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameYear(then) -> getFormatter("MMM d h:mm a")
            else -> getFormatter("MMM d yyyy h:mm a")
        }.format(date)
    }
}
