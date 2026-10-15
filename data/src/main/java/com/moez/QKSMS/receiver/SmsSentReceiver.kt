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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.interactor.MarkFailed
import org.prauga.messages.interactor.MarkSent
import org.prauga.messages.worker.SmsSendRetryWorker
import timber.log.Timber

class SmsSentReceiver : BroadcastReceiver() {

    @Inject lateinit var markSent: MarkSent
    @Inject lateinit var markFailed: MarkFailed

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val id = intent.getLongExtra(SmsSentParts.EXTRA_ID, 0L)
        val outcome =
            parts.onResult(
                id = id,
                attempt = intent.getLongExtra(SmsSentParts.EXTRA_ATTEMPT, 0L),
                part = intent.getIntExtra(SmsSentParts.EXTRA_PART, 0),
                parts = intent.getIntExtra(SmsSentParts.EXTRA_PARTS, 1),
                ok = resultCode == Activity.RESULT_OK,
            )

        when (outcome) {
            SmsSentParts.Outcome.NONE,
            SmsSentParts.Outcome.FAILED_SILENTLY -> Unit

            SmsSentParts.Outcome.SENT -> {
                SmsSendRetryWorker.clear(context, id)
                val pendingResult = goAsync()
                markSent.execute(id) { pendingResult.finish() }
            }

            SmsSentParts.Outcome.FAILED -> {
                val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1)
                if (
                    SmsSendRetry.isTemporary(resultCode, errorCode) &&
                        SmsSendRetryWorker.schedule(context, id)
                ) {
                    Timber.v("SMS $id failed with $resultCode ($errorCode), retrying later")
                    return
                }

                SmsSendRetryWorker.clear(context, id)
                val pendingResult = goAsync()
                markFailed.execute(MarkFailed.Params(id, resultCode)) { pendingResult.finish() }
            }
        }
    }

    companion object {
        private const val EXTRA_ERROR_CODE = "errorCode"

        private val parts = SmsSentParts()
    }
}
