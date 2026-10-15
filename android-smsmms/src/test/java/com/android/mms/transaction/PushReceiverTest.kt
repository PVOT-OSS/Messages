/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.android.mms.transaction

import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.NotificationInd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PushReceiverTest {

    private fun notificationFrom(address: String?) =
        NotificationInd().apply { address?.let { from = EncodedStringValue(it) } }

    @Test
    fun givenTypedAddress_whenSenderAddress_thenDropsTheTypeSuffix() {
        assertEquals(
            "+15551234567",
            PushReceiver.senderAddress(notificationFrom("+15551234567/TYPE=PLMN")),
        )
    }

    @Test
    fun givenPlainAddressOrEmail_whenSenderAddress_thenKeepsIt() {
        assertEquals("+15551234567", PushReceiver.senderAddress(notificationFrom("+15551234567")))
        assertEquals("a@example.com", PushReceiver.senderAddress(notificationFrom("a@example.com")))
    }

    @Test
    fun givenNoSender_whenSenderAddress_thenNull() {
        assertNull(PushReceiver.senderAddress(notificationFrom(null)))
    }
}
