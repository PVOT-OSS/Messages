/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.Telephony
import androidx.core.content.edit
import dagger.android.AndroidInjection
import java.io.File
import org.prauga.messages.interactor.SendMessage
import org.prauga.messages.model.Attachment
import timber.log.Timber

class TestMmsPushReceiver : MmsReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SEND -> return send(context, intent)
            ACTION_DOWNLOAD -> return download(context, intent)
            ACTION_SET_PREF -> return setPref(context, intent)
        }

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

    private fun send(context: Context, intent: Intent) {
        val addresses = intent.getStringExtra("to")?.split(',')?.filter { it.isNotBlank() }
        if (addresses.isNullOrEmpty()) {
            Timber.w("No recipients to send to")
            return
        }

        val sender = RemoteMessagingReceiver().also { AndroidInjection.inject(it, context) }
        val attachments =
            if (intent.getBooleanExtra("image", false)) listOf(testImage(context)) else listOf()
        val params =
            SendMessage.Params(
                subId = intent.getIntExtra(EXTRA_SUBSCRIPTION, -1),
                threadId = 0,
                addresses = addresses,
                body = intent.getStringExtra("text").orEmpty(),
                attachments = attachments,
            )

        Timber.v("Sending a test message to $addresses")
        val pendingResult = goAsync()
        Thread {
                try {
                    sender.sendMessage.buildObservable(params).blockingSubscribe({}, Timber::w)
                } finally {
                    pendingResult.finish()
                }
            }
            .start()
    }

    private fun download(context: Context, intent: Intent) {
        val uri = intent.getStringExtra("uri")?.let(Uri::parse) ?: return
        val repo = RemoteMessagingReceiver().also { AndroidInjection.inject(it, context) }.messageRepo
        val pendingResult = goAsync()
        Thread {
                try {
                    repo.downloadMms(uri, intent.getIntExtra(EXTRA_SUBSCRIPTION, -1), false)
                } finally {
                    pendingResult.finish()
                }
            }
            .start()
    }

    private fun setPref(context: Context, intent: Intent) {
        val key = intent.getStringExtra("key") ?: return
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            if (intent.hasExtra("value")) putBoolean(key, intent.getBooleanExtra("value", false))
            else remove(key)
        }
    }

    private fun testImage(context: Context): Attachment {
        val file = File(context.cacheDir, "test-mms-image.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.MAGENTA)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Attachment(context, Uri.fromFile(file))
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
        private const val ACTION_SEND = "org.prauga.messages.TEST_MMS_SEND"
        private const val ACTION_DOWNLOAD = "org.prauga.messages.TEST_MMS_DOWNLOAD"
        private const val ACTION_SET_PREF = "org.prauga.messages.TEST_SET_PREF"
        private const val EXTRA_PDU = "pdu"
        private const val EXTRA_SUBSCRIPTION = "subscription"
    }
}
