/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import androidx.core.app.NotificationCompat

interface NotificationManager {

    fun update(threadId: Long)

    fun notifyFailed(threadId: Long)

    fun createNotificationChannel(threadId: Long = 0L)

    fun buildNotificationChannelId(threadId: Long): String

    fun getNotificationForBackup(): NotificationCompat.Builder

    fun cancel(i: Int)

    fun showDeleteConfirmation(threadId: Long)
}
