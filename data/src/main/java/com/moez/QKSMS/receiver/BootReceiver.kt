/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.ResumeUnsentSms
import org.prauga.messages.interactor.UpdateScheduledMessageAlarms
import org.prauga.messages.worker.SmsSendRetryWorker

class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var resumeUnsentSms: ResumeUnsentSms
    @Inject lateinit var updateScheduledMessageAlarms: UpdateScheduledMessageAlarms

    override fun onReceive(context: Context, intent: Intent?) {
        AndroidInjection.inject(this, context)

        val result = goAsync()
        val retrying = SmsSendRetryWorker.pending(context) - SmsSendRetryWorker.inFlight(context)
        resumeUnsentSms.execute(ResumeUnsentSms.Params(System.currentTimeMillis(), retrying)) {
            updateScheduledMessageAlarms.execute(Unit) { result.finish() }
        }
    }
}
