/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.util.NightModeManager

class NightModeReceiver : BroadcastReceiver() {

    @Inject lateinit var nightModeManager: NightModeManager

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        nightModeManager.updateCurrentTheme()
    }
}
