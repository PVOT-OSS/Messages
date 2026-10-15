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
import org.prauga.messages.interactor.SyncMessage

/**
 * When the SMS contentprovider is changed by a process other than us, we need to sync the Uri that
 * was changed.
 *
 * This can happen if a message is sent through something like Pushbullet or Google Assistant.
 *
 * This only works on API 24+, so to fully solve this problem we'll need a smarter way of running
 * partial syncs on older devices.
 *
 * https://developer.android.com/reference/android/provider/Telephony.Sms.Intents.html#ACTION_EXTERNAL_PROVIDER_CHANGE
 */
class SmsProviderChangedReceiver : BroadcastReceiver() {

    @Inject lateinit var syncMessage: SyncMessage

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        // Obtain the uri for the changed data
        // If the value is null, don't continue
        val uri = intent.data ?: return

        // Sync the message to our realm
        val pendingResult = goAsync()
        syncMessage.execute(uri) { pendingResult.finish() }
    }
}
