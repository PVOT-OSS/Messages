/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import dagger.android.AndroidInjection

class StartActivityFromWidgetReceiver : BroadcastReceiver() {
    // why does this shim class exist rather than the widget launching activities more directly?
    // the main widget listview items all need to launch the compose activity at an existing
    // threadId *except* the 'view more conversations' footer item, which needs to launch
    // the main activity. a listview in a widget *must* use a single pending intent template
    // for all items which doesn't allow for more than one component/class. this shim class
    // allows a single component/class for the widget listview pending intent template that, based
    // on the activityToStart value, launches the appropriate activity

    companion object {
        const val COMPOSE_ACTIVITY = ".feature.compose.ComposeActivity"
        const val MAIN_ACTIVITY = ".feature.main.MainActivity"
    }

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        var activityToStartName = intent.getStringExtra("activityToStart")

        // protect against invalid launch activity
        activityToStartName =
            when (activityToStartName) {
                COMPOSE_ACTIVITY -> activityToStartName
                MAIN_ACTIVITY -> activityToStartName
                else -> return
            }

        context.startActivity(
            Intent(context, Class.forName(context.packageName + activityToStartName))
                .setFlags(FLAG_ACTIVITY_NEW_TASK)
                .putExtra("threadId", intent.getLongExtra("threadId", 0L))
        )
    }
}
