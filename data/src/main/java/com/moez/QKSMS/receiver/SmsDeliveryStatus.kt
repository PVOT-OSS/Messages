/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.provider.Telephony.Sms

object SmsDeliveryStatus {

    const val FORMAT_3GPP2 = "3gpp2"

    private const val CDMA_ERROR_NONE = 0
    private const val CDMA_ERROR_TEMPORARY = 2
    private const val CDMA_STATUS_DELIVERED = 2

    fun from(status: Int, format: String?): Int =
        when (format) {
            FORMAT_3GPP2 -> {
                val errorClass = (status shr 24) and 0x03
                val code = (status shr 16) and 0x3f
                when (errorClass) {
                    CDMA_ERROR_NONE ->
                        if (code == CDMA_STATUS_DELIVERED) Sms.STATUS_COMPLETE else Sms.STATUS_PENDING
                    CDMA_ERROR_TEMPORARY -> Sms.STATUS_PENDING
                    else -> Sms.STATUS_FAILED
                }
            }

            else ->
                when {
                    status < Sms.STATUS_PENDING -> Sms.STATUS_COMPLETE
                    status < Sms.STATUS_FAILED -> Sms.STATUS_PENDING
                    else -> Sms.STATUS_FAILED
                }
        }
}
