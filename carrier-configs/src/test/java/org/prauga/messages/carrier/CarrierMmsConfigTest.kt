/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.carrier

import android.os.Bundle
import android.util.Xml
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CarrierMmsConfigTest {

    private fun parse(xml: String): Bundle =
        Bundle().also { bundle ->
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            CarrierMmsConfig.parse(parser, bundle)
        }

    private fun config(vararg values: Pair<String, Any>) =
        CarrierMmsConfig(
            Bundle().apply {
                values.forEach { (key, value) ->
                    when (value) {
                        is Int -> putInt(key, value)
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                    }
                }
            }
        )

    @Test
    fun givenTypedEntries_whenParse_thenReadsEachType() {
        val values =
            parse(
                """
                <mms_config version="1">
                    <bool name="enabledMMS">false</bool>
                    <int name="maxMessageSize">1048576</int>
                    <string name="httpParams">X-Header: value</string>
                </mms_config>
                """
            )

        assertEquals(false, values.get("enabledMMS"))
        assertEquals(1048576, values.get("maxMessageSize"))
        assertEquals("X-Header: value", values.get("httpParams"))
    }

    @Test
    fun givenWhitespaceAroundValues_whenParse_thenTrimsThem() {
        val values =
            parse(
                """
                <motorola_config_override>
                    <bool name="groupChatDefaultsToMMS">true
                    </bool>
                </motorola_config_override>
                """
            )

        assertEquals(true, values.get("groupChatDefaultsToMMS"))
    }

    @Test
    fun givenInvalidIntOrEmptyString_whenParse_thenSkipsOnlyThatEntry() {
        val values =
            parse(
                """
                <mms_config>
                    <int name="maxMessageSize">big</int>
                    <string name="naiSuffix"></string>
                    <int name="recipientLimit">10</int>
                </mms_config>
                """
            )

        assertFalse(values.containsKey("maxMessageSize"))
        assertFalse(values.containsKey("naiSuffix"))
        assertEquals(10, values.get("recipientLimit"))
    }

    @Test
    fun givenNoValues_whenRead_thenUsesGoogleMessagesDefaults() {
        val config = config()

        assertTrue(config.mmsEnabled)
        assertTrue(config.groupMmsEnabled)
        assertEquals(300 * 1024, config.maxMessageSize)
        assertEquals(Int.MAX_VALUE, config.recipientLimit)
        assertEquals(-1, config.smsToMmsTextThreshold)
        assertTrue(config.autoRetrieveEnabled)
        assertFalse(config.roamingAutoRetrieveEnabled)
        assertTrue(config.supportMmsContentDisposition)
    }

    @Test
    fun givenMmsDisabled_whenRead_thenGroupMmsAndThresholdAreOff() {
        val config =
            config(
                CarrierMmsConfig.KEY_MMS_ENABLED to false,
                CarrierMmsConfig.KEY_SMS_TO_MMS_TEXT_THRESHOLD to 4,
            )

        assertFalse(config.groupMmsEnabled)
        assertEquals(-1, config.smsToMmsTextThreshold)
    }

    @Test
    fun givenNoMultipartSms_whenRead_thenSecondPartSwitchesToMms() {
        val config = config(CarrierMmsConfig.KEY_MULTIPART_SMS_ENABLED to false)

        assertEquals(1, config.smsToMmsTextThreshold)
    }

    @Test
    fun givenNegativeRecipientLimit_whenRead_thenUnlimited() {
        assertEquals(
            Int.MAX_VALUE,
            config(CarrierMmsConfig.KEY_RECIPIENT_LIMIT to -1).recipientLimit,
        )
        assertEquals(10, config(CarrierMmsConfig.KEY_RECIPIENT_LIMIT to 10).recipientLimit)
    }

    @Test
    fun givenDefaultSubscription_whenResolve_thenUsesDefaultSmsSim() {
        assertEquals(-1, CarrierMmsConfig.resolveSubscriptionId(-5))
        assertEquals(3, CarrierMmsConfig.resolveSubscriptionId(3))
    }
}
