/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.app.Activity
import android.telephony.SmsManager
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduParser
import com.google.android.mms.pdu_alt.SendConf
import timber.log.Timber

enum class MmsRetry {
    NONE,
    AUTO,
    MANUAL,
    NEVER,
}

data class MmsSendResult(
    val retry: MmsRetry,
    val messageId: String? = null,
    val responseStatus: Int = 0,
) {
    companion object {
        private const val ERROR_SATELLITE_OVERSIZE = 13
        private const val ERROR_DATA_STALL = 112

        const val RESULT_TOO_LARGE = 10_000

        @JvmStatic
        fun from(
            resultCode: Int,
            httpStatus: Int,
            response: ByteArray?,
            noService: Boolean = false,
        ): MmsSendResult =
            when (resultCode) {
                Activity.RESULT_OK -> fromResponse(response)
                else -> MmsSendResult(retryForError(resultCode, httpStatus, noService))
            }

        private fun fromResponse(response: ByteArray?): MmsSendResult {
            val sendConf =
                response
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        try {
                            PduParser(it).parse() as? SendConf
                        } catch (e: Exception) {
                            Timber.w(e, "Unable to parse the M-Send.conf")
                            null
                        }
                    } ?: return MmsSendResult(MmsRetry.NONE)

            val status = sendConf.responseStatus
            val messageId = sendConf.messageId?.let { String(it) }?.takeIf { it.isNotBlank() }
            val retry =
                when (status) {
                    PduHeaders.RESPONSE_STATUS_OK -> MmsRetry.NONE
                    PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_FAILURE,
                    PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_NETWORK_PROBLEM,
                    PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_PARTIAL_SUCCESS -> MmsRetry.AUTO
                    else -> MmsRetry.NEVER
                }
            return MmsSendResult(retry, messageId, status)
        }

        @JvmStatic
        @JvmOverloads
        fun retryForError(resultCode: Int, httpStatus: Int, noService: Boolean = false): MmsRetry =
            when (resultCode) {
                Activity.RESULT_OK -> MmsRetry.NONE
                SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS ->
                    if (noService) MmsRetry.AUTO else MmsRetry.MANUAL
                RESULT_TOO_LARGE -> MmsRetry.NEVER
                SmsManager.MMS_ERROR_HTTP_FAILURE ->
                    if (httpStatus == 404) MmsRetry.NEVER else MmsRetry.AUTO
                SmsManager.MMS_ERROR_IO_ERROR,
                ERROR_SATELLITE_OVERSIZE,
                ERROR_DATA_STALL -> MmsRetry.AUTO
                else -> MmsRetry.MANUAL
            }
    }
}
