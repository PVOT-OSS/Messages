/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.Telephony
import androidx.core.content.edit
import dagger.android.AndroidInjection
import io.realm.Realm
import java.io.ByteArrayOutputStream
import java.io.File
import org.prauga.messages.interactor.DeleteMessages
import org.prauga.messages.interactor.SendMessage
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Message
import timber.log.Timber

class TestMmsPushReceiver : MmsReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SEND -> return send(context, intent)
            ACTION_DOWNLOAD -> return download(context, intent)
            ACTION_SET_PREF -> return setPref(context, intent)
            ACTION_BLOCK -> return block(context, intent)
            ACTION_DELETE -> return delete(context, intent)
            ACTION_RETRY -> return retry(context, intent)
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
        val kinds =
            intent.getStringExtra("attach")?.split(',')?.filter { it.isNotBlank() }.orEmpty() +
                listOfNotNull("png".takeIf { intent.getBooleanExtra("image", false) })
        val attachments = kinds.mapIndexed { index, kind -> testAttachment(context, kind, index) }
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

    private fun testAttachment(context: Context, kind: String, index: Int): Attachment {
        fun bitmap(size: Int, format: Bitmap.CompressFormat, color: Int) =
            ByteArrayOutputStream().also { out ->
                Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(color) }
                    .compress(format, 100, out)
            }.toByteArray()

        val (extension, bytes) =
            when (kind) {
                "jpeg" -> "jpg" to bitmap(64, Bitmap.CompressFormat.JPEG, Color.BLUE)
                "bigimage" -> "jpg" to noisyJpeg(2000)
                "gif" -> "gif" to GIF
                "video" -> "mp4" to MP4
                "audio" -> "amr" to AMR
                "vcard" -> "vcf" to "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Test\r\nTEL:+15559876543\r\nEND:VCARD\r\n".toByteArray()
                "pdf" -> "pdf" to "%PDF-1.4\n%%EOF\n".toByteArray()
                "bigfile" -> "bin" to ByteArray(2 * 1024 * 1024) { it.toByte() }
                else -> "png" to bitmap(64, Bitmap.CompressFormat.PNG, Color.MAGENTA)
            }
        val file = File(context.cacheDir, "test-attachment-$index.$extension")
        file.writeBytes(bytes)
        return Attachment(context, Uri.fromFile(file))
    }

    private fun noisyJpeg(size: Int): ByteArray {
        val random = java.util.Random(1)
        val pixels = IntArray(size * size) { random.nextInt() or 0xFF000000.toInt() }
        val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream()
            .also { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            .toByteArray()
    }

    private fun block(context: Context, intent: Intent) {
        val address = intent.getStringExtra("address") ?: return
        val client =
            BlockThreadReceiver().also { AndroidInjection.inject(it, context) }.blockingClient
        runInBackground {
            when (intent.getBooleanExtra("unblock", false)) {
                true -> client.unblock(listOf(address)).blockingAwait()
                false -> client.block(listOf(address)).blockingAwait()
            }
        }
    }

    private fun delete(context: Context, intent: Intent) {
        val remote = RemoteMessagingReceiver().also { AndroidInjection.inject(it, context) }
        val deleteMessages =
            DeleteMessagesReceiver().also { AndroidInjection.inject(it, context) }.deleteMessages
        val thread = intent.getLongExtra("thread", -1)
        val message = intent.getStringExtra("uri")?.let(Uri::parse)?.let(::findMessage)
        runInBackground {
            when {
                thread != -1L -> remote.conversationRepo.deleteConversations(thread)
                message != null ->
                    deleteMessages
                        .buildObservable(DeleteMessages.Params(listOf(message.first), message.second))
                        .blockingSubscribe({}, Timber::w)
            }
        }
    }

    private fun retry(context: Context, intent: Intent) {
        val message = intent.getStringExtra("uri")?.let(Uri::parse)?.let(::findMessage) ?: return
        val retrySending =
            SendSmsReceiver().also { AndroidInjection.inject(it, context) }.retrySending
        runInBackground {
            retrySending.buildObservable(message.first).blockingSubscribe({}, Timber::w)
        }
    }

    private fun findMessage(uri: Uri): Pair<Long, Long>? =
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm
                .where(Message::class.java)
                .equalTo("type", "mms")
                .equalTo("contentId", ContentUris.parseId(uri))
                .findFirst()
                ?.let { it.id to it.threadId }
        }

    private fun runInBackground(work: () -> Unit) {
        val pendingResult = goAsync()
        Thread {
                try {
                    work()
                } catch (e: Exception) {
                    Timber.w(e)
                } finally {
                    pendingResult.finish()
                }
            }
            .start()
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
        private const val ACTION_BLOCK = "org.prauga.messages.TEST_BLOCK"
        private const val ACTION_DELETE = "org.prauga.messages.TEST_DELETE"
        private const val ACTION_RETRY = "org.prauga.messages.TEST_RETRY"

        private val GIF =
            byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 1, 0, 1, 0, 0x80.toByte(), 0, 0, 0, 0, 0,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x21, 0xF9.toByte(), 4, 1, 0, 0, 0, 0,
                0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 0x44, 1, 0, 0x3B)
        private val MP4 = "\u0000\u0000\u0000\u0018ftypmp42\u0000\u0000\u0000\u0000mp42isom".toByteArray()
        private val AMR = "#!AMR\n".toByteArray() + ByteArray(160) { if (it % 32 == 0) 0x3C else 0 }
        private const val EXTRA_PDU = "pdu"
        private const val EXTRA_SUBSCRIPTION = "subscription"
    }
}
