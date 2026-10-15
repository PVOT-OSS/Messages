/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.mms

import android.content.Context
import android.content.res.Configuration
import android.telephony.SmsManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.carrier.MmsConfigOverrides

@RunWith(AndroidJUnit4::class)
class CarrierMmsConfigInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun givenCarrierWithItsOwnConfig_whenBundled_thenUsesThatCarriersValues() {
        val values = CarrierMmsConfig.bundled(context, 310, 260)

        assertEquals(1048576, values.get("maxMessageSize"))
        assertEquals(12, values.get("smsToMmsTextThreshold"))
        assertEquals(true, values.get("groupChatDefaultsToMMS"))
    }

    @Test
    fun givenMncZeroZero_whenBundled_thenUsesTheMnc00Folder() {
        val values = CarrierMmsConfig.bundled(context, 450, Configuration.MNC_ZERO)

        assertEquals(1048576, values.get("maxMessageSize"))
        assertEquals(true, values.get("groupChatDefaultsToMMS"))
    }

    @Test
    fun givenUnknownCarrier_whenBundled_thenFallsBackToTheDefaults() {
        val values = CarrierMmsConfig.bundled(context, 999, 99)

        assertEquals(307200, values.get("maxMessageSize"))
        assertEquals(false, values.get("groupChatDefaultsToMMS"))
        assertEquals(true, values.get("enabledMMS"))
    }

    @Test
    fun givenTheDeviceSim_whenGet_thenTheSystemCarrierConfigWins() {
        val platform = SmsManager.getDefault().carrierConfigValues
        val config = CarrierMmsConfig.get(context, -1)

        if (platform.containsKey(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE)) {
            assertEquals(
                platform.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE),
                config.maxMessageSize,
            )
        }
        if (platform.containsKey(SmsManager.MMS_CONFIG_MMS_ENABLED)) {
            assertEquals(platform.getBoolean(SmsManager.MMS_CONFIG_MMS_ENABLED), config.mmsEnabled)
        }
    }

    @Test
    fun givenTheDeviceSim_whenOverrides_thenOnlyAddsWhatTheSystemLacksPlusUserAgent() {
        val platform = SmsManager.getDefault().carrierConfigValues
        val overrides = MmsConfigOverrides.create(context, -1)

        assertNotNull(overrides.getString(SmsManager.MMS_CONFIG_USER_AGENT))
        assertTrue(overrides.getString(SmsManager.MMS_CONFIG_UA_PROF_URL)!!.isNotBlank())
        overrides
            .keySet()
            .filter {
                it != SmsManager.MMS_CONFIG_USER_AGENT && it != SmsManager.MMS_CONFIG_UA_PROF_URL
            }
            .forEach { key ->
                assertFalse("$key is already set by the system", platform.containsKey(key))
            }
    }
}
