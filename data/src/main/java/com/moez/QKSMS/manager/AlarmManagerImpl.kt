/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import org.prauga.messages.receiver.SendScheduledMessageReceiver

class AlarmManagerImpl @Inject constructor(private val context: Context) : AlarmManager {

    override fun getScheduledMessageIntent(id: Long): PendingIntent {
        val intent = Intent(context, SendScheduledMessageReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun setAlarm(date: Long, intent: PendingIntent) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, date, intent)
    }
}
