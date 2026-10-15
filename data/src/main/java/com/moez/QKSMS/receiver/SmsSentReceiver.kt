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
            SmsSentParts.Outcome.NONE -> Unit

            SmsSentParts.Outcome.SENT -> {
                val pendingResult = goAsync()
                markSent.execute(id) { pendingResult.finish() }
            }

            SmsSentParts.Outcome.FAILED,
            SmsSentParts.Outcome.FAILED_SILENTLY -> {
                val pendingResult = goAsync()
                val notify = outcome == SmsSentParts.Outcome.FAILED
                markFailed.execute(MarkFailed.Params(id, resultCode, notify)) {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private val parts = SmsSentParts()
    }
}
