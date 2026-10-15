/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

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
        val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1)
        val part =
            SmsSentParts.Result(SmsSendRetry.category(resultCode, errorCode), resultCode, errorCode)
        val result =
            parts(context)
                .onResult(
                    id = id,
                    attempt = intent.getLongExtra(SmsSentParts.EXTRA_ATTEMPT, 0L),
                    parts = intent.getIntExtra(SmsSentParts.EXTRA_PARTS, 1),
                    result = part,
                ) ?: return

        val retrying =
            result.category == SmsSentParts.Category.AUTO_RETRY &&
                SmsSendRetryWorker.schedule(context, id)

        when {
            result.category == SmsSentParts.Category.SENT -> {
                SmsSendRetryWorker.clear(context, id)
                val pendingResult = goAsync()
                markSent.execute(id) { pendingResult.finish() }
            }

            retrying ->
                Timber.v("SMS $id failed with ${result.resultCode} (${result.errorCode}), retrying")

            else -> {
                SmsSendRetryWorker.clear(context, id)
                val pendingResult = goAsync()
                markFailed.execute(MarkFailed.Params(id, result.resultCode)) {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val EXTRA_ERROR_CODE = "errorCode"

        private var instance: SmsSentParts? = null

        @Synchronized
        private fun parts(context: Context): SmsSentParts =
            instance
                ?: SmsSentParts(PreferencesStore(context, "sms_send_parts")).also { instance = it }
    }

    private class PreferencesStore(context: Context, name: String) : SmsSentParts.Store {
        private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

        override fun get(key: String) = prefs.getString(key, null)

        // Committed right away: the next part may come in a process started from scratch
        override fun put(key: String, value: String) {
            prefs.edit().putString(key, value).commit()
        }

        override fun remove(key: String) {
            prefs.edit().remove(key).commit()
        }
    }
}
