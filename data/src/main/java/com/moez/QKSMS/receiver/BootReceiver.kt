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
