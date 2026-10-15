/*
 * SPDX-FileCopyrightText: 2013 Jacob Klinker
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.klinker.android.send_message

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.google.android.mms.ContentType
import com.google.android.mms.InvalidHeaderValueException
import com.google.android.mms.MMSPart
import com.google.android.mms.pdu_alt.CharacterSets
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.PduBody
import com.google.android.mms.pdu_alt.PduComposer
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduPart
import com.google.android.mms.pdu_alt.PduPersister
import com.google.android.mms.pdu_alt.SendReq
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.carrier.MmsConfigOverrides
import timber.log.Timber

/**
 * Class to process transaction requests for sending
 *
 * @author Jake Klinker
 */
class Transaction
@JvmOverloads
constructor(private val context: Context, settings: Settings = Settings()) {

    companion object {
        var settings: Settings = Settings()

        const val EXTRA_CONTENT_URI = "content_uri"
        const val EXTRA_FILE_PATH = "file_path"

        const val MMS_ERROR = "com.klinker.android.send_message.MMS_ERROR"
        const val REFRESH = "com.klinker.android.send_message.REFRESH"
        const val MMS_PROGRESS = "com.klinker.android.send_message.MMS_PROGRESS"
        const val NOTIFY_OF_DELIVERY = "com.klinker.android.send_message.NOTIFY_DELIVERY"
        const val NOTIFY_OF_MMS = "com.klinker.android.messaging.NEW_MMS_DOWNLOADED"

        const val DEFAULT_EXPIRY_TIME = (7 * 24 * 60 * 60).toLong()
        const val DEFAULT_PRIORITY = PduHeaders.PRIORITY_NORMAL
    }

    init {
        Transaction.settings = settings
    }

    /**
     * Called to send a new message depending on settings and provided Message object If you want to
     * send message as mms, call this from the UI thread
     *
     * @param threadId is the thread id of who to send the message to (can also be set to
     *   Transaction.NO_THREAD_ID)
     */
    fun sendNewMessage(
        subId: Int,
        threadId: Long,
        addresses: List<String>,
        parts: List<MMSPart>,
        subject: String?,
        existingUri: Uri?,
        maxMessageSize: Int? = null,
    ) {
        Timber.v("sending mms")
        var messageUri: Uri? = existingUri
        var sentPI: PendingIntent? = null
        try {
            val fileName = "send." + Math.abs(Random().nextLong()).toString() + ".dat"
            val sendFile = File(context.cacheDir, fileName)

            val sendReq = buildPdu(context, subId, addresses, subject, parts)
            val persister = PduPersister.getPduPersister(context)
            val uri =
                existingUri
                    ?: persister.persist(
                        sendReq,
                        Uri.parse("content://mms/outbox"),
                        threadId,
                        true,
                        true,
                        null,
                    )
            messageUri = uri
            sentPI = sentIntent(uri, sendFile)

            val updatedAction = "${context.packageName}.MMS_UPDATED"
            val updatedIntent = Intent(updatedAction).putExtra("uri", uri.toString())
            BroadcastUtils.addClassName(context, updatedIntent, updatedAction)
            context.sendBroadcast(updatedIntent)

            val sizeLimit = maxMessageSize ?: CarrierMmsConfig.get(context, subId).maxMessageSize
            val pdu = PduComposer(context, sendReq).make()
            if (pdu == null) {
                Timber.e("Unable to compose MMS")
                sentPI.send(SmsManager.MMS_ERROR_UNSPECIFIED)
                return
            }
            if (pdu.size > sizeLimit) {
                Timber.w("MMS is ${pdu.size} bytes, more than the $sizeLimit the carrier accepts")
                sentPI.send(MmsSendResult.RESULT_TOO_LARGE)
                return
            }

            val contentUri: Uri? =
                try {
                    FileOutputStream(sendFile).use { writer ->
                        writer.write(pdu)
                        Uri.Builder()
                            .authority(context.packageName + ".MmsFileProvider")
                            .path(fileName)
                            .scheme(ContentResolver.SCHEME_CONTENT)
                            .build()
                    }
                } catch (e: IOException) {
                    Timber.e(e, "Error writing send file")
                    null
                } catch (e: OutOfMemoryError) {
                    Timber.e(e, "Not enough memory to compose MMS")
                    null
                }

            val configOverrides =
                MmsConfigOverrides.create(context, subId).apply {
                    maxMessageSize?.let { putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, it) }
                }

            if (contentUri != null) {
                SmsManagerFactory.createSmsManager(CarrierMmsConfig.resolveSubscriptionId(subId))
                    .sendMultimediaMessage(context, contentUri, null, configOverrides, sentPI)
            } else {
                Timber.e("Error writing sending Mms")
                try {
                    sentPI.send(SmsManager.MMS_ERROR_IO_ERROR)
                } catch (e: PendingIntent.CanceledException) {
                    Timber.e(e)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error using system sending method")
            try {
                (sentPI ?: messageUri?.let { sentIntent(it, null) })?.send(
                    SmsManager.MMS_ERROR_UNSPECIFIED
                )
            } catch (e: Exception) {
                Timber.e(e, "Unable to report the failed MMS")
            }
        }
    }

    private fun sentIntent(messageUri: Uri, sendFile: File?): PendingIntent {
        val sentAction = "${context.packageName}.MMS_SENT"
        val sentIntent = Intent(sentAction)
        BroadcastUtils.addClassName(context, sentIntent, sentAction)

        sentIntent.putExtra(EXTRA_CONTENT_URI, messageUri.toString())
        sendFile?.let { sentIntent.putExtra(EXTRA_FILE_PATH, it.path) }
        val mutable =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            messageUri.hashCode(),
            sentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
    }

    private fun buildPdu(
        context: Context,
        subId: Int,
        recipients: List<String>,
        subject: String?,
        parts: List<MMSPart>,
    ): SendReq {
        val req = SendReq()

        Utils.getMyPhoneNumber(context, subId)
            ?.takeIf(String::isNotEmpty)
            ?.let(::EncodedStringValue)
            ?.let(req::setFrom)
        recipients.map(::EncodedStringValue).forEach(req::addTo) // To
        subject
            ?.takeIf(String::isNotEmpty)
            ?.let(::EncodedStringValue)
            ?.let(req::setSubject) // Subject

        req.date = System.currentTimeMillis() / 1000
        req.body = PduBody()

        // Parts
        parts.map(this::partToPduPart).forEach { req.body.addPart(it) }

        val smil = SmilBuilder.build(parts.map { SmilBuilder.Part(it.mimeType, it.name) })
        req.body.addPart(
            0,
            PduPart().apply {
                contentId = "smil".toByteArray()
                contentLocation = "smil.xml".toByteArray()
                contentType = ContentType.APP_SMIL.toByteArray()
                data = smil.toByteArray()
            },
        )

        req.messageSize = parts.mapNotNull { it.data?.size }.sum().toLong()
        req.messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()
        req.expiry = DEFAULT_EXPIRY_TIME

        try {
            req.priority = DEFAULT_PRIORITY
            req.deliveryReport = PduHeaders.VALUE_NO
            req.readReport = PduHeaders.VALUE_NO
        } catch (e: InvalidHeaderValueException) {
            Timber.w(e)
        }

        return req
    }

    private fun partToPduPart(part: MMSPart): PduPart =
        PduPart().apply {
            val filename = part.name

            // Set Charset if it's a text media.
            if (part.mimeType.startsWith("text")) {
                charset = CharacterSets.UTF_8
            }

            // Set Content-Type.
            contentType = part.mimeType.toByteArray()

            // Set Content-Location.
            contentLocation = filename.toByteArray()
            val index = filename.lastIndexOf(".")
            contentId = (if (index == -1) filename else filename.substring(0, index)).toByteArray()
            data = part.data
        }
}
