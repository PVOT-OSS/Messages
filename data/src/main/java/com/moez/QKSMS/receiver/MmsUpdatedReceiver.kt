/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.SyncMessage

class MmsUpdatedReceiver : BroadcastReceiver() {

    companion object {
        const val URI = "uri"
    }

    @Inject lateinit var syncMessage: SyncMessage

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        intent.getStringExtra(URI)?.let { uriString ->
            val pendingResult = goAsync()
            syncMessage.execute(Uri.parse(uriString)) { pendingResult.finish() }
        }
    }
}
