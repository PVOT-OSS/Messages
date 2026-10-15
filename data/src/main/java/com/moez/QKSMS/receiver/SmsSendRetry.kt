/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.app.Activity
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

    fun category(resultCode: Int, errorCode: Int): SmsSentParts.Category =
        when {
            resultCode == Activity.RESULT_OK -> SmsSentParts.Category.SENT
            isTemporary(resultCode, errorCode) -> SmsSentParts.Category.AUTO_RETRY
            else -> SmsSentParts.Category.MANUAL_RETRY
        }
}
