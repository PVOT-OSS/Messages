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
import com.klinker.android.send_message.MmsRetry
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.ReceiveMms
import org.prauga.messages.interactor.SyncMessage
import org.prauga.messages.util.runAfter
import org.prauga.messages.worker.MmsDownloadRetryWorker

class MmsReceivedReceiver : MmsReceivedReceiver() {

    @Inject lateinit var receiveMms: ReceiveMms
    @Inject lateinit var syncMessage: SyncMessage

    private var context: Context? = null
    private var notificationUri: Uri? = null
    private var messageUri: Uri? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        AndroidInjection.inject(this, context)
        this.context = context
        val notificationUri = intent?.getParcelableExtra<Uri>(EXTRA_URI)
        this.notificationUri = notificationUri

        super.onReceive(context, intent)

        val pendingResult = goAsync()
        val finish = runAfter(2) { pendingResult.finish() }

        notificationUri?.let { syncMessage.execute(it, finish) } ?: finish()
        messageUri?.let { receiveMms.execute(it, finish) } ?: finish()
    }

    override fun onMessageReceived(messageUri: Uri?) {
        this.messageUri = messageUri
        notificationUri?.let {
            context?.let { context -> MmsDownloadRetryWorker.clear(context, it) }
        }
    }

    override fun onDownloadFailed(notificationUri: Uri?, subId: Int, retry: MmsRetry) {
        val context = context ?: return
        val uri = notificationUri ?: return
        when (retry) {
            MmsRetry.AUTO -> MmsDownloadRetryWorker.schedule(context, uri, subId)
            else -> MmsDownloadRetryWorker.clear(context, uri)
        }
    }
}
