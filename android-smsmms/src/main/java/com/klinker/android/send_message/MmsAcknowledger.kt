/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.android.mms.pdu_alt.AcknowledgeInd
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.GenericPdu
import com.google.android.mms.pdu_alt.NotifyRespInd
import com.google.android.mms.pdu_alt.PduComposer
import com.google.android.mms.pdu_alt.PduHeaders
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.carrier.MmsConfigOverrides
import timber.log.Timber

object MmsAcknowledger {

    private const val FILE_PREFIX = "mms-ack-"

    @JvmStatic
    fun sendNotifyResp(
        context: Context,
        subId: Int,
        transactionId: ByteArray,
        status: Int,
        contentLocation: String?,
    ) {
        send(context, subId, contentLocation) {
            NotifyRespInd(PduHeaders.CURRENT_MMS_VERSION, transactionId, status)
        }
    }

    @JvmStatic
    fun sendAcknowledge(
        context: Context,
        subId: Int,
        transactionId: ByteArray,
        contentLocation: String?,
    ) {
        send(context, subId, contentLocation) {
            AcknowledgeInd(PduHeaders.CURRENT_MMS_VERSION, transactionId).apply {
                from = EncodedStringValue(Utils.getMyPhoneNumber(context, subId).orEmpty())
            }
        }
    }

    private fun send(
        context: Context,
        subscriptionId: Int,
        contentLocation: String?,
        pdu: () -> GenericPdu,
    ) {
        try {
            val subId = CarrierMmsConfig.resolveSubscriptionId(subscriptionId)
            removeOldFiles(context)

            val bytes = PduComposer(context, pdu()).make() ?: return
            val fileName = "$FILE_PREFIX${UUID.randomUUID()}.dat"
            File(context.cacheDir, fileName).writeBytes(bytes)
            val contentUri =
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(context.packageName + ".MmsFileProvider")
                    .path(fileName)
                    .build()

            val url = contentLocation.takeIf { CarrierMmsConfig.get(context, subId).notifyWapMmsc }

            SmsManagerFactory.createSmsManager(subId)
                .sendMultimediaMessage(
                    context,
                    contentUri,
                    url,
                    MmsConfigOverrides.create(context, subId),
                    null,
                )
        } catch (e: Exception) {
            Timber.w(e, "Unable to acknowledge MMS")
        }
    }

    private fun removeOldFiles(context: Context) {
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        context.cacheDir
            .listFiles { file -> file.name.startsWith(FILE_PREFIX) && file.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}
