/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.provider.Telephony.Sms
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsDeliveryStatusTest {

    private fun gsm(status: Int) = SmsDeliveryStatus.from(status, "3gpp")

    private fun cdma(errorClass: Int, code: Int) =
        SmsDeliveryStatus.from(((errorClass shl 8) or code) shl 16, SmsDeliveryStatus.FORMAT_3GPP2)

    @Test
    fun givenGsmReceivedBySme_whenFrom_thenDelivered() {
        assertEquals(Sms.STATUS_COMPLETE, gsm(0x00))
        assertEquals(Sms.STATUS_COMPLETE, gsm(0x02))
    }

    @Test
    fun givenGsmTemporaryErrorStillTrying_whenFrom_thenPending() {
        assertEquals(Sms.STATUS_PENDING, gsm(0x20))
        assertEquals(Sms.STATUS_PENDING, gsm(0x3f))
    }

    @Test
    fun givenGsmPermanentOrGivenUpError_whenFrom_thenFailed() {
        assertEquals(Sms.STATUS_FAILED, gsm(0x40))
        assertEquals(Sms.STATUS_FAILED, gsm(0x46))
        assertEquals(Sms.STATUS_FAILED, gsm(0x60))
    }

    @Test
    fun givenNoFormat_whenFrom_thenTreatedAsGsm() {
        assertEquals(Sms.STATUS_COMPLETE, SmsDeliveryStatus.from(0, null))
        assertEquals(Sms.STATUS_FAILED, SmsDeliveryStatus.from(0x41, null))
    }

    @Test
    fun givenCdmaDelivered_whenFrom_thenDelivered() {
        assertEquals(Sms.STATUS_COMPLETE, SmsDeliveryStatus.from(0x20000, SmsDeliveryStatus.FORMAT_3GPP2))
        assertEquals(Sms.STATUS_COMPLETE, cdma(errorClass = 0, code = 2))
    }

    @Test
    fun givenCdmaAcceptedButNotYetDelivered_whenFrom_thenPending() {
        assertEquals(Sms.STATUS_PENDING, cdma(errorClass = 0, code = 0))
    }

    @Test
    fun givenCdmaTemporaryError_whenFrom_thenPending() {
        assertEquals(Sms.STATUS_PENDING, cdma(errorClass = 2, code = 4))
    }

    @Test
    fun givenCdmaPermanentError_whenFrom_thenFailed() {
        assertEquals(Sms.STATUS_FAILED, cdma(errorClass = 3, code = 4))
    }
}
