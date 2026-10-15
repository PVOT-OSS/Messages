/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import android.app.PendingIntent

interface AlarmManager {

    fun getScheduledMessageIntent(id: Long): PendingIntent

    fun setAlarm(date: Long, intent: PendingIntent)
}
