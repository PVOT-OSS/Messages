/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.klinker.android.send_message.BroadcastUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import org.prauga.messages.util.Preferences

class WidgetManagerImpl
@Inject
constructor(private val context: Context, private val prefs: Preferences) : WidgetManager {

    companion object {
        private var staticUnreadAtTopPrefsDisposable = AtomicReference<Disposable>(null)

        fun sendDatasetChanged(context: Context) {
            BroadcastUtils.sendExplicitBroadcast(
                context,
                Intent(),
                WidgetManager.ACTION_NOTIFY_DATASET_CHANGED,
            )
        }
    }

    init {
        // this class is always instantiated by it's factory with the application context so we can
        // capture that context value below
        // when unreadAtTop preference changes, send broadcast to widgets to update themselves
        if (staticUnreadAtTopPrefsDisposable.get() === null)
            staticUnreadAtTopPrefsDisposable.set(
                prefs.unreadAtTop
                    .asObservable()
                    .skip(1)
                    .debounce(400, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { sendDatasetChanged(context) }
                    .subscribe()
            )
    }

    override fun sendDatasetChanged() {
        sendDatasetChanged(context)
    }

    override fun updateTheme() {
        val ids =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(
                    ComponentName(
                        context.packageName,
                        "org.prauga.messages.feature.widget.WidgetProvider",
                    )
                )

        val intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        BroadcastUtils.sendExplicitBroadcast(
            context,
            intent,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
        )
    }
}
