/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.provider.Telephony.Sms

object SmsDeliveryStatus {

    const val FORMAT_3GPP2 = "3gpp2"

    private const val CDMA_DELIVERED = 0x20000

    fun from(status: Int, format: String?): Int? =
        when (if (format == FORMAT_3GPP2 && status == CDMA_DELIVERED) 0 else status) {
            0 -> Sms.STATUS_COMPLETE
            Sms.STATUS_FAILED -> Sms.STATUS_FAILED
            else -> null
        }
}
