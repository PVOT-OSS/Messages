/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.klinker.android.send_message.MmsReceivedReceiver
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.ReceiveMms
import org.prauga.messages.interactor.SyncMessage
import org.prauga.messages.util.runAfter

class MmsReceivedReceiver : MmsReceivedReceiver() {

    @Inject lateinit var receiveMms: ReceiveMms
    @Inject lateinit var syncMessage: SyncMessage

    private var messageUri: Uri? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        AndroidInjection.inject(this, context)
        val notificationUri = intent?.getParcelableExtra<Uri>(EXTRA_URI)

        super.onReceive(context, intent)

        val pendingResult = goAsync()
        val finish = runAfter(2) { pendingResult.finish() }

        notificationUri?.let { syncMessage.execute(it, finish) } ?: finish()
        messageUri?.let { receiveMms.execute(it, finish) } ?: finish()
    }

    override fun onMessageReceived(messageUri: Uri?) {
        this.messageUri = messageUri
    }
}
