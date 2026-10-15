/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.android.AndroidInjection
import timber.log.Timber

class TestMmsPushReceiver : MmsReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pdu = intent.getStringExtra(EXTRA_PDU)?.hexToBytes()
        if (pdu == null) {
            Timber.w("No PDU to deliver")
            return
        }

        Timber.v("Delivering a test MMS notification of ${pdu.size} bytes")
        val push =
            Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION)
                .setType("application/vnd.wap.mms-message")
                .putExtra("data", pdu)
        if (intent.hasExtra(EXTRA_SUBSCRIPTION)) {
            push.putExtra(EXTRA_SUBSCRIPTION, intent.getIntExtra(EXTRA_SUBSCRIPTION, -1))
        }
        super.onReceive(context, push)
    }

    override fun inject(context: Context) {
        val injected = MmsReceiver()
        AndroidInjection.inject(injected, context)
        mmsDownloadPolicy = injected.mmsDownloadPolicy
        receiveMms = injected.receiveMms
    }

    private fun String.hexToBytes(): ByteArray? =
        takeIf { it.length % 2 == 0 }
            ?.chunked(2)
            ?.map { it.toIntOrNull(16)?.toByte() ?: return null }
            ?.toByteArray()

    companion object {
        private const val EXTRA_PDU = "pdu"
        private const val EXTRA_SUBSCRIPTION = "subscription"
    }
}
