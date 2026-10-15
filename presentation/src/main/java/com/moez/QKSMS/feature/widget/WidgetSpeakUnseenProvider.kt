/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.prauga.messages.R
import org.prauga.messages.receiver.SpeakThreadsReceiver

class WidgetSpeakUnseenProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) updateWidget(context, appWidgetId)
    }

    private fun updateWidget(context: Context?, appWidgetId: Int) {
        super.onEnabled(context)

        if (context == null) return

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_speak_unseen)

        remoteViews.setImageViewResource(R.id.speakUnseenImage, R.drawable.ic_speak_unseen_widget)

        // speak unseen intent
        val speakUnseenIntent =
            Intent(context, SpeakThreadsReceiver::class.java).putExtra("threadId", -1L)
        val speakUnseenPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                speakUnseenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        remoteViews.setOnClickPendingIntent(R.id.speakUnseenImage, speakUnseenPendingIntent)

        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
    }
}
