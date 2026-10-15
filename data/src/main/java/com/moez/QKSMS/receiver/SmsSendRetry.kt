/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.telephony.SmsManager

object SmsSendRetry {

    private val TEMPORARY_NETWORK_ERRORS = setOf(41, 332)

    fun isTemporary(resultCode: Int, errorCode: Int): Boolean =
        when (resultCode) {
            SmsManager.RESULT_ERROR_RADIO_OFF,
            SmsManager.RESULT_ERROR_NO_SERVICE -> true

            SmsManager.RESULT_NETWORK_ERROR,
            SmsManager.RESULT_RIL_NETWORK_ERR -> errorCode in TEMPORARY_NETWORK_ERRORS

            else -> false
        }
}
