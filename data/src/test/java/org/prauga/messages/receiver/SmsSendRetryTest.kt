/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.app.Activity
import android.telephony.SmsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSendRetryTest {

    @Test
    fun givenRadioOffOrNoService_whenChecked_thenTemporary() {
        assertTrue(SmsSendRetry.isTemporary(SmsManager.RESULT_ERROR_RADIO_OFF, -1))
        assertTrue(SmsSendRetry.isTemporary(SmsManager.RESULT_ERROR_NO_SERVICE, 0))
    }

    @Test
    fun givenNetworkErrorWithATemporaryCause_whenChecked_thenTemporary() {
        assertTrue(SmsSendRetry.isTemporary(SmsManager.RESULT_NETWORK_ERROR, 41))
        assertTrue(SmsSendRetry.isTemporary(SmsManager.RESULT_RIL_NETWORK_ERR, 332))
    }

    @Test
    fun givenNetworkErrorWithAnyOtherCause_whenChecked_thenNotTemporary() {
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_NETWORK_ERROR, -1))
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_RIL_NETWORK_ERR, 38))
    }

    @Test
    fun givenAPermanentFailure_whenChecked_thenNotTemporary() {
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_ERROR_GENERIC_FAILURE, 41))
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_ERROR_NULL_PDU, -1))
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_ERROR_LIMIT_EXCEEDED, -1))
        assertFalse(SmsSendRetry.isTemporary(SmsManager.RESULT_NETWORK_REJECT, -1))
    }

    @Test
    fun givenSuccess_whenChecked_thenNotTemporary() {
        assertFalse(SmsSendRetry.isTemporary(Activity.RESULT_OK, -1))
    }

    @Test
    fun givenEachKindOfResult_whenCategorised_thenSentAutoOrManual() {
        assertEquals(SmsSentParts.Category.SENT, SmsSendRetry.category(Activity.RESULT_OK, -1))
        assertEquals(
            SmsSentParts.Category.AUTO_RETRY,
            SmsSendRetry.category(SmsManager.RESULT_ERROR_NO_SERVICE, -1),
        )
        assertEquals(
            SmsSentParts.Category.MANUAL_RETRY,
            SmsSendRetry.category(SmsManager.RESULT_ERROR_GENERIC_FAILURE, -1),
        )
    }
}
