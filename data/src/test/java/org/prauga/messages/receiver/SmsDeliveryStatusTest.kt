/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import android.provider.Telephony.Sms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsDeliveryStatusTest {

    private fun gsm(status: Int) = SmsDeliveryStatus.from(status, "3gpp")

    private fun cdma(status: Int) = SmsDeliveryStatus.from(status, SmsDeliveryStatus.FORMAT_3GPP2)

    @Test
    fun givenReceivedBySme_whenFrom_thenDelivered() {
        assertEquals(Sms.STATUS_COMPLETE, gsm(0x00))
        assertEquals(Sms.STATUS_COMPLETE, SmsDeliveryStatus.from(0, null))
    }

    @Test
    fun givenExactly64_whenFrom_thenFailed() {
        assertEquals(Sms.STATUS_FAILED, gsm(0x40))
    }

    @Test
    fun givenAnyOtherStatus_whenFrom_thenNothingChanges() {
        assertNull(gsm(0x02))
        assertNull(gsm(0x20))
        assertNull(gsm(0x46))
        assertNull(gsm(0x60))
    }

    @Test
    fun givenCdmaDelivered_whenFrom_thenDelivered() {
        assertEquals(Sms.STATUS_COMPLETE, cdma(0x20000))
    }

    @Test
    fun givenAnyOtherCdmaStatus_whenFrom_thenNothingChanges() {
        assertNull(cdma(0x3040000))
        assertNull(cdma(0x2040000))
    }
}
