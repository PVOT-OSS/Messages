/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import timber.log.Timber

object MmsHandoff {

    const val EXTRA_ATTEMPT = "mms_handoff_attempt"
    const val EXTRA_TIMED_OUT = "mms_handoff_timed_out"

    enum class Kind {
        SEND,
        DOWNLOAD,
    }

    private val PROBE_TIMEOUT = TimeUnit.SECONDS.toMillis(60)
    private val DEADLINE = TimeUnit.MINUTES.toMillis(10)

    private const val STAGE_PROBE = "probe"
    private const val STAGE_DEADLINE = "deadline"

    private const val KEY_KIND = "kind"
    private const val KEY_KEY = "key"
    private const val KEY_ATTEMPT = "attempt"
    private const val KEY_SUB_ID = "subId"
    private const val KEY_STAGE = "stage"
    private const val KEY_RESULT = "result"
    private const val KEY_URI_EXTRAS = "mms_handoff_uri_extras"

    fun newAttempt(): Long = System.currentTimeMillis()

    fun start(
        context: Context,
        kind: Kind,
        key: String,
        subId: Int,
        attempt: Long,
        result: Intent,
    ) {
        try {
            state(context).start(name(kind, key), attempt, subId)
            val data =
                Data.Builder()
                    .putString(KEY_KIND, kind.name)
                    .putString(KEY_KEY, key)
                    .putLong(KEY_ATTEMPT, attempt)
                    .putInt(KEY_SUB_ID, subId)
                    .putString(KEY_RESULT, encode(result))
                    .build()
            val workManager = WorkManager.getInstance(context)
            listOf(STAGE_PROBE to 0L, STAGE_DEADLINE to DEADLINE).forEach { (stage, delay) ->
                val request =
                    OneTimeWorkRequestBuilder<MmsHandoffWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            Data.Builder().putAll(data).putString(KEY_STAGE, stage).build()
                        )
                        .build()
                workManager.enqueueUniqueWork(
                    work(kind, key, stage),
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Unable to watch the MMS ${kind.name.lowercase()} of $key")
        }
    }

    fun result(
        context: Context,
        kind: Kind,
        key: String,
        intent: Intent,
        succeeded: Boolean,
    ): MmsHandoffState.Outcome {
        val attempt = intent.getLongExtra(EXTRA_ATTEMPT, -1L).takeIf { it != -1L }
        val timedOut = intent.getBooleanExtra(EXTRA_TIMED_OUT, false)
        val outcome = state(context).result(name(kind, key), attempt, timedOut, succeeded)
        if (!timedOut && outcome != MmsHandoffState.Outcome.STALE) cancel(context, kind, key)
        if (outcome != MmsHandoffState.Outcome.CURRENT) {
            Timber.v("${outcome.name} result for the MMS ${kind.name.lowercase()} of $key")
        }
        return outcome
    }

    internal fun check(context: Context, input: Data) {
        val kind = input.getString(KEY_KIND)?.let(Kind::valueOf) ?: return
        val key = input.getString(KEY_KEY) ?: return
        val attempt = input.getLong(KEY_ATTEMPT, -1L)
        val subId = input.getInt(KEY_SUB_ID, -1)
        val result = input.getString(KEY_RESULT) ?: return
        val state = state(context)
        if (!state.isWaiting(name(kind, key), attempt)) return

        when (input.getString(KEY_STAGE)) {
            STAGE_PROBE -> {
                if (!state.probeTrusted(subId)) return
                val network = MmsNetworkProbe(context).await(subId, PROBE_TIMEOUT)
                Timber.v("MMS network for the ${kind.name.lowercase()} of $key: $network")
                if (network == MmsNetworkProbe.Result.UNAVAILABLE) {
                    timeOut(context, kind, key, attempt, result, noNetwork = true)
                }
            }
            STAGE_DEADLINE -> {
                Timber.w("No answer for the MMS ${kind.name.lowercase()} of $key")
                timeOut(context, kind, key, attempt, result, noNetwork = false)
            }
        }
    }

    private fun timeOut(
        context: Context,
        kind: Kind,
        key: String,
        attempt: Long,
        result: String,
        noNetwork: Boolean,
    ) {
        if (!state(context).timeOut(name(kind, key), attempt, noNetwork)) return
        cancel(context, kind, key, if (noNetwork) STAGE_DEADLINE else STAGE_PROBE)
        val intent = decode(result).putExtra(EXTRA_ATTEMPT, attempt).putExtra(EXTRA_TIMED_OUT, true)
        context.sendOrderedBroadcast(
            intent,
            null,
            null,
            null,
            SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS,
            null,
            null,
        )
    }

    private fun cancel(context: Context, kind: Kind, key: String, vararg stages: String) {
        try {
            val workManager = WorkManager.getInstance(context)
            stages
                .ifEmpty { arrayOf(STAGE_PROBE, STAGE_DEADLINE) }
                .forEach { stage -> workManager.cancelUniqueWork(work(kind, key, stage)) }
        } catch (e: Exception) {
            Timber.w(e, "Unable to stop watching the MMS ${kind.name.lowercase()} of $key")
        }
    }

    private fun state(context: Context) =
        MmsHandoffState(context.getSharedPreferences("mms_handoffs", Context.MODE_PRIVATE))

    private fun name(kind: Kind, key: String) = "${kind.name.lowercase()}:$key"

    private fun work(kind: Kind, key: String, stage: String) =
        "mms-handoff-$stage:${name(kind, key)}"

    @Suppress("DEPRECATION")
    internal fun encode(intent: Intent): String {
        val extras = intent.extras
        val uriKeys = extras?.keySet()?.filter { extras.get(it) is Uri }.orEmpty()
        return Intent(intent)
            .apply {
                uriKeys.forEach { putExtra(it, extras?.get(it).toString()) }
                putExtra(KEY_URI_EXTRAS, uriKeys.joinToString(","))
            }
            .toUri(Intent.URI_INTENT_SCHEME)
    }

    internal fun decode(value: String): Intent =
        Intent.parseUri(value, Intent.URI_INTENT_SCHEME).apply {
            getStringExtra(KEY_URI_EXTRAS)?.split(',')?.filter(String::isNotEmpty)?.forEach { key ->
                getStringExtra(key)?.let { putExtra(key, Uri.parse(it)) }
            }
            removeExtra(KEY_URI_EXTRAS)
        }
}

class MmsHandoffWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        MmsHandoff.check(applicationContext, inputData)
        return Result.success()
    }
}
