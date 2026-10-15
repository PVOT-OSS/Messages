/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
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
